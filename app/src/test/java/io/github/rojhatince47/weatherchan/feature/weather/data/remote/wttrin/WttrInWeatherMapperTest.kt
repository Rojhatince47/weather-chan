package io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WttrInWeatherMapperTest {

    @Test
    fun `yedek kaynak yerel saat ve astronomiye gore geceyi tanir`() {
        val city = City("New York", "New York", "ABD", Coordinates(40.71, -74.0))
        val dto = WttrInDto(
            currentCondition = listOf(WttrInCurrentDto(observationTime = "11:05 PM")),
            weather = listOf(
                WttrInDayDto(
                    date = "2026-11-06",
                    astronomy = listOf(WttrInAstronomyDto(sunrise = "06:30 AM", sunset = "04:45 PM"))
                )
            )
        )

        val mapper = WttrInWeatherMapper()

        assertFalse(mapper.toCityWeather(city, dto).currentWeather.isDay)
        assertFalse(mapper.toForecast(city, dto).currentWeather.isDay)
    }

    @Test
    fun `yedek kaynak gunduz varsayar`() {
        // observation_time veya astronomi yoksa saat-türetme yapılamaz;
        // gündüz varsayımı bilinçli yedektir.
        val city = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
        val mapper = WttrInWeatherMapper()
        assertTrue(mapper.toCityWeather(city, WttrInDto()).currentWeather.isDay)
    }

    @Test
    fun `hafif ciselti yumusak kategoriye duser`() {
        // WMO 51'in karşılığı: 266 hafif çisenti.
        val city = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
        val mapper = WttrInWeatherMapper()
        val dto = WttrInDto(
            currentCondition = listOf(WttrInCurrentDto(weatherCode = "266"))
        )
        assertEquals(
            WeatherCondition.LIGHT_SHOWERS,
            mapper.toCityWeather(city, dto).currentWeather.condition
        )
    }

    @Test
    fun `saatlik girisler astronomi penceresine gore isDay tasir`() {
        val city = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
        val dto = WttrInDto(
            weather = listOf(
                WttrInDayDto(
                    date = "2026-10-08",
                    astronomy = listOf(WttrInAstronomyDto(sunrise = "05:43 AM", sunset = "06:31 PM")),
                    hourly = listOf(
                        WttrInHourlyDto(time = "1200", tempC = "20", weatherCode = "113", chanceOfRain = "10"),
                        WttrInHourlyDto(time = "2000", tempC = "15", weatherCode = "113", chanceOfRain = "10")
                    )
                )
            )
        )

        val forecast = WttrInWeatherMapper().toForecast(city, dto)

        assertTrue(forecast.hourlyForecast[0].isDay)
        assertFalse(forecast.hourlyForecast[1].isDay)
    }

    @Test
    fun `astronomi yoksa 06 18 kurali gecer`() {
        val city = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
        val dto = WttrInDto(
            weather = listOf(
                WttrInDayDto(
                    date = "2026-10-08",
                    hourly = listOf(
                        WttrInHourlyDto(time = "0900", tempC = "20", weatherCode = "113", chanceOfRain = "0"),
                        WttrInHourlyDto(time = "2300", tempC = "15", weatherCode = "113", chanceOfRain = "0")
                    )
                )
            )
        )

        val forecast = WttrInWeatherMapper().toForecast(city, dto)

        assertTrue(forecast.hourlyForecast[0].isDay)
        assertFalse(forecast.hourlyForecast[1].isDay)
    }

    @Test
    fun `uc gunun dilimleri damgali duzlesir`() {
        val city = City("İstanbul", null, "Türkiye", Coordinates(41.01, 28.98))
        val day = { d: String, hours: List<String> ->
            WttrInDayDto(
                date = d,
                astronomy = listOf(WttrInAstronomyDto(sunrise = "06:30 AM", sunset = "06:30 PM")),
                hourly = hours.map { h -> WttrInHourlyDto(time = h, tempC = "20", weatherCode = "113", chanceOfRain = "0") }
            )
        }
        val dto = WttrInDto(
            weather = listOf(
                day("2026-10-08", listOf("0", "300", "600", "900", "1200", "1500", "1800", "2100")),
                day("2026-10-09", listOf("0", "300")),
                day("2026-10-10", listOf("0"))
            )
        )
        val forecast = WttrInWeatherMapper().toForecast(city, dto)
        assertEquals(11, forecast.hourlyForecast.size)
        assertEquals(java.time.LocalDateTime.of(2026, 10, 10, 0, 0), forecast.hourlyForecast.last().localDateTime)
    }

    @Test
    fun `gun bazli astronomi lookup her gunu kendi penceresine bakar`() {
        // Gün 0 batım 18:30 → 18:00 dilimi GÜNDÜZ; gün 2 batım 17:30 → 18:00 dilimi GECE.
        // Tek pencere (gün 0) düzleştirmede ikisini de true yapardı.
        val city = City("İstanbul", null, "Türkiye", Coordinates(41.01, 28.98))
        val day = { d: String, sunset: String ->
            WttrInDayDto(
                date = d,
                astronomy = listOf(WttrInAstronomyDto(sunrise = "06:30 AM", sunset = sunset)),
                hourly = listOf(WttrInHourlyDto(time = "1800", tempC = "20", weatherCode = "113", chanceOfRain = "0"))
            )
        }
        val dto = WttrInDto(
            weather = listOf(day("2026-10-08", "06:30 PM"), day("2026-10-09", "06:00 PM"), day("2026-10-10", "05:30 PM"))
        )
        val forecast = WttrInWeatherMapper().toForecast(city, dto)
        assertTrue(forecast.hourlyForecast[0].isDay)
        assertFalse(forecast.hourlyForecast[2].isDay)
    }

    @Test
    fun `eksik veya bozuk time ve date dilimi eler`() {
        val city = City("İstanbul", null, "Türkiye", Coordinates(41.01, 28.98))
        val dto = WttrInDto(
            weather = listOf(
                WttrInDayDto(
                    date = "2026-10-08",
                    hourly = listOf(
                        WttrInHourlyDto(time = "600", tempC = "20", weatherCode = "113", chanceOfRain = "0"),
                        WttrInHourlyDto(time = null, tempC = "20", weatherCode = "113", chanceOfRain = "0"),
                        WttrInHourlyDto(time = "ABC", tempC = "20", weatherCode = "113", chanceOfRain = "0")
                    )
                ),
                WttrInDayDto(
                    hourly = listOf(WttrInHourlyDto(time = "600", tempC = "20", weatherCode = "113", chanceOfRain = "0"))
                )
            )
        )
        val forecast = WttrInWeatherMapper().toForecast(city, dto)
        // Yalnız 06:00 kalır; `"0"` ise MEŞRU 00:00'dır, elenmez.
        assertEquals(1, forecast.hourlyForecast.size)
    }
}

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
    fun `yedek kaynak gunduz varsayar`() {
        // Yedek yolda saat-türetme yoktur (bilinçli kapsam kesiti); veri doğru,
        // sunum gündüz varsayımıyla çizilir.
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
}

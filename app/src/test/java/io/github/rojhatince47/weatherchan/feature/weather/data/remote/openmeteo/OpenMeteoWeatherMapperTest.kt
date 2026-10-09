package io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoWeatherMapperTest {

    private val mapper = OpenMeteoWeatherMapper()
    private val city = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))

    private fun dto(isDay: Int?) = OpenMeteoForecastDto(
        current = OpenMeteoCurrentDto(
            temperature = 20.0,
            humidity = 50,
            weatherCode = 0,
            windSpeed = 5.0,
            isDay = isDay
        )
    )

    @Test
    fun `is-day-bir-gece-degildir`() {
        // Open-Meteo `is_day`: 1 = gündüz, 0 = gece.
        assertTrue(mapper.toCityWeather(city, dto(isDay = 1)).currentWeather.isDay)
    }

    @Test
    fun `is-day-sifir-gecedir`() {
        assertFalse(mapper.toCityWeather(city, dto(isDay = 0)).currentWeather.isDay)
    }

    @Test
    fun `is-day yoksa gunduz varsayilir`() {
        assertTrue(mapper.toCityWeather(city, dto(isDay = null)).currentWeather.isDay)
    }

    @Test
    fun `hafif kodlar yumusak kategoriye duser`() {
        // 51 (hafif çisenti) ve 80 (hafif sağanak): eser miktarı tam yağmur
        // gibi göstermek kullanıcı gözünde yanlıştır.
        listOf(51, 80).forEach { code ->
            val dto = OpenMeteoForecastDto(
                current = OpenMeteoCurrentDto(
                    temperature = 20.0,
                    humidity = 50,
                    weatherCode = code,
                    windSpeed = 5.0,
                    isDay = 1
                )
            )
            assertEquals(
                WeatherCondition.LIGHT_SHOWERS,
                mapper.toCityWeather(city, dto).currentWeather.condition
            )
        }
    }

    private fun forecastDto(times: List<String>) = OpenMeteoForecastDto(
        current = OpenMeteoCurrentDto(),
        hourly = OpenMeteoHourlyDto(
            time = times,
            temperature = List(times.size) { 20.0 },
            weatherCode = List(times.size) { 0 },
            precipitationProbability = List(times.size) { 10 }
        ),
        daily = OpenMeteoDailyDto(
            time = listOf("2026-10-08"),
            sunrise = listOf("2026-10-08T06:32"),
            sunset = listOf("2026-10-08T19:45")
        )
    )

    @Test
    fun `saatlik girisler gun dogus batis penceresine gore isDay tasir`() {
        val forecast = mapper.toForecast(city, forecastDto(listOf("2026-10-08T12:00", "2026-10-08T20:00")))

        assertTrue(forecast.hourlyForecast[0].isDay)
        assertFalse(forecast.hourlyForecast[1].isDay)
    }

    @Test
    fun `gun verisi yoksa 06 18 kurali gecer`() {
        val dto = forecastDto(listOf("2026-10-08T09:00", "2026-10-08T23:00")).copy(
            daily = OpenMeteoDailyDto()
        )

        val forecast = mapper.toForecast(city, dto)

        assertTrue(forecast.hourlyForecast[0].isDay)
        assertFalse(forecast.hourlyForecast[1].isDay)
    }
}

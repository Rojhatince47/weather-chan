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
}

package io.github.rojhatince47.weatherchan.feature.weather.data.local

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.HourlyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CachedForecastDtoTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val ankara = City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85))
    private val forecast = Forecast(
        city = ankara,
        currentWeather = CurrentWeather(14, WeatherCondition.CLOUDY, 6, 45),
        hourlyForecast = listOf(HourlyForecast(10, 15, WeatherCondition.CLEAR, 5)),
        dailyForecast = listOf(DailyForecast(0, 8, 16, WeatherCondition.PARTLY_CLOUDY))
    )

    @Test
    fun `gidis donus kayipsizdir`() {
        val dto = CachedForecastDto(1_750_000_000_000L, forecast.toDto())
        val raw = json.encodeToString(CachedForecastDto.serializer(), dto)
        val back = json.decodeFromString<CachedForecastDto>(raw).toDomain()
        assertEquals(1_750_000_000_000L, back.fetchedAt.toEpochMilli())
        assertEquals(forecast, back.forecast)
    }

    @Test
    fun `bilinmeyen kosul cozumu patlatir`() {
        // Giriş-başı yakalama repository katmanındadır; DTO dönüşümü fırlatır.
        val current = forecast.toDto().current.copy(condition = "FIRTINA")
        val dto = CachedForecastDto(1L, forecast.toDto().copy(current = current))
        var threw = false
        try {
            dto.toDomain()
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertEquals(true, threw)
    }

    @Test
    fun `gece bayragi gidis donuste korunur`() {
        val night = forecast.copy(currentWeather = forecast.currentWeather.copy(isDay = false))
        val back = CachedForecastDto(1L, night.toDto()).toDomain()
        assertEquals(false, back.forecast.currentWeather.isDay)
    }
}

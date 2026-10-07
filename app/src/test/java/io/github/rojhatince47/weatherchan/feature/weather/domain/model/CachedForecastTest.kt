package io.github.rojhatince47.weatherchan.feature.weather.domain.model

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedForecastTest {

    private val fetchedAt = Instant.parse("2026-10-02T10:00:00Z")

    private fun anyForecast() = Forecast(
        city = City("X", null, null, Coordinates(0.0, 0.0)),
        currentWeather = CurrentWeather(0, WeatherCondition.CLEAR, 0, 0),
        hourlyForecast = emptyList(),
        dailyForecast = emptyList()
    )

    @Test
    fun `yirmidokuz dakika ellidokuz saniye tazedir`() {
        assertFalse(CachedForecast(fetchedAt, anyForecast()).isStale(fetchedAt.plusSeconds(29 * 60 + 59)))
    }

    @Test
    fun `otuz dakika bayattir`() {
        assertTrue(CachedForecast(fetchedAt, anyForecast()).isStale(fetchedAt.plusSeconds(30 * 60)))
    }

    @Test
    fun `gelecek damga bayattir`() {
        assertTrue(CachedForecast(fetchedAt, anyForecast()).isStale(fetchedAt.minusSeconds(1)))
    }
}

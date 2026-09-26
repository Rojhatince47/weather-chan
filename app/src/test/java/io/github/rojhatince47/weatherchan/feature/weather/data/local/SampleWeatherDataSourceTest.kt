package io.github.rojhatince47.weatherchan.feature.weather.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleWeatherDataSourceTest {
    private val dataSource = SampleWeatherDataSource()

    @Test
    fun `sample data contains five cities`() {
        assertEquals(5, dataSource.cities.size)
    }

    @Test
    fun `each city has current weather`() {
        val cityWeathers = dataSource.cityWeathers

        assertEquals(5, cityWeathers.size)
        assertTrue(cityWeathers.all { it.currentWeather.temperatureCelsius > -50 })
    }

    @Test
    fun `forecast contains twenty four hourly and seven daily items`() {
        val forecast = dataSource.forecast(cityId = 1L)

        assertEquals(24, forecast.hourlyForecast.size)
        assertEquals(7, forecast.dailyForecast.size)
    }
}

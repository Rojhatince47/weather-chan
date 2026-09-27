package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.weather.data.local.SampleWeatherDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherUiMapperTest {
    private val mapper = WeatherUiMapper()
    private val dataSource = SampleWeatherDataSource()

    @Test
    fun `maps city weather to ui model`() {
        val cityWeather = dataSource.cityWeathers.first()
        val uiModel = mapper.toCityWeatherUiModel(cityWeather, isFavorite = false)

        assertEquals(cityWeather.city.id, uiModel.cityId)
        assertEquals(cityWeather.city.name, uiModel.name)
        assertTrue(uiModel.temperatureText.endsWith("°C"))
        assertEquals(cityWeather.currentWeather.condition, uiModel.condition)
        assertEquals(
            UiText.Resource(R.string.condition_clear),
            uiModel.conditionLabel
        )
    }

    @Test
    fun `maps forecast to detail ui model`() {
        val forecast = dataSource.forecast(cityId = 1L)
        val uiModel = mapper.toForecastDetailUiModel(forecast, isFavorite = false)

        assertEquals(24, uiModel.hourlyForecast.size)
        assertEquals(7, uiModel.dailyForecast.size)
        assertTrue(uiModel.currentWeather.temperatureText.endsWith("°C"))
        assertEquals(
            UiText.Resource(R.string.forecast_today),
            uiModel.dailyForecast.first().dateText
        )
        assertEquals(0, uiModel.dailyForecast.first().dayOffset)
    }

    @Test
    fun `converts temperatures to fahrenheit`() {
        val cityWeather = dataSource.cityWeathers.first()
        val uiModel = mapper.toCityWeatherUiModel(
            cityWeather,
            isFavorite = false,
            unit = TemperatureUnit.FAHRENHEIT
        )
        val expected = cityWeather.currentWeather.temperatureCelsius * 9 / 5 + 32

        assertEquals("$expected°F", uiModel.temperatureText)
    }
}

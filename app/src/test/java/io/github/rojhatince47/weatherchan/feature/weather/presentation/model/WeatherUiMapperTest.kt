package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.HourlyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherUiMapperTest {
    private val mapper = WeatherUiMapper()

    private val istanbul = City(
        name = "İstanbul",
        region = "Marmara",
        country = "Türkiye",
        coordinates = Coordinates(latitude = 41.01, longitude = 28.98)
    )

    private fun cityWeather() = CityWeather(
        city = istanbul,
        currentWeather = CurrentWeather(
            temperatureCelsius = 18,
            condition = WeatherCondition.CLEAR,
            windSpeedKmh = 8,
            humidityPercent = 50
        )
    )

    private fun forecast() = Forecast(
        city = istanbul,
        currentWeather = CurrentWeather(
            temperatureCelsius = 18,
            condition = WeatherCondition.CLEAR,
            windSpeedKmh = 8,
            humidityPercent = 50
        ),
        hourlyForecast = List(24) { hour ->
            HourlyForecast(
                hourOfDay = hour,
                temperatureCelsius = 15 + hour % 10,
                condition = WeatherCondition.PARTLY_CLOUDY,
                precipitationProbabilityPercent = hour * 3 % 100
            )
        },
        dailyForecast = List(7) { day ->
            DailyForecast(
                dayOffset = day,
                minTemperatureCelsius = 10 + day % 5,
                maxTemperatureCelsius = 20 + day % 8,
                condition = WeatherCondition.CLOUDY
            )
        }
    )

    @Test
    fun `maps city weather to ui model`() {
        val cityWeather = cityWeather()
        val uiModel = mapper.toCityWeatherUiModel(cityWeather)
        val weather = uiModel.weather as CardWeather.Loaded

        assertEquals(cityWeather.city.id, uiModel.cityId)
        assertEquals(cityWeather.city.name, uiModel.name)
        assertTrue(weather.temperatureText.endsWith("°C"))
        assertEquals(cityWeather.currentWeather.condition, weather.condition)
        assertEquals(
            UiText.Resource(R.string.condition_clear),
            weather.conditionLabel
        )
    }

    /**
     * Hava durumu alınamayan şehir de listede kalır; kart sıcaklık yerine
     * nedeni ve eylemi gösterir. Yüksüz kartın varlığı bu eşlemeye bağlıdır.
     */
    @Test
    fun `maps city without weather to unavailable card`() {
        val uiModel = mapper.toUnavailable(istanbul)

        assertEquals(istanbul.id, uiModel.cityId)
        assertEquals("İstanbul", uiModel.name)
        assertEquals("Marmara", uiModel.region)
        assertEquals("Türkiye", uiModel.country)
        assertTrue(uiModel.weather is CardWeather.Unavailable)
    }

    @Test
    fun `maps forecast to detail ui model`() {
        val forecast = forecast()
        val uiModel = mapper.toForecastDetailUiModel(forecast)

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
        val cityWeather = cityWeather()
        val uiModel = mapper.toCityWeatherUiModel(
            cityWeather,
            unit = TemperatureUnit.FAHRENHEIT
        )
        val expected = cityWeather.currentWeather.temperatureCelsius * 9 / 5 + 32
        val weather = uiModel.weather as CardWeather.Loaded

        assertEquals("$expected°F", weather.temperatureText)
    }
}

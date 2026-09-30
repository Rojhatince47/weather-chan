package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import javax.inject.Inject

class WeatherUiMapper @Inject constructor() {

    fun toCityWeatherUiModel(cityWeather: CityWeather, unit: TemperatureUnit = TemperatureUnit.CELSIUS): CityWeatherUiModel =
        CityWeatherUiModel(
            cityId = cityWeather.city.id,
            name = cityWeather.city.name,
            region = cityWeather.city.region,
            country = cityWeather.city.country,
            weather = CardWeather.Loaded(
                temperatureText = formatTemperature(cityWeather.currentWeather.temperatureCelsius, unit),
                condition = cityWeather.currentWeather.condition,
                conditionEmoji = emojiFor(cityWeather.currentWeather.condition),
                conditionLabel = labelFor(cityWeather.currentWeather.condition)
            ),
            isLocation = cityWeather.city.isLocation
        )

    /**
     * Hava durumu alınamayan şehir için kart üretir. Kayıt listede kaldığı
     * sürece bu kart görünür; [CardWeather.Unavailable] dalı nedeni ve eylemi
     * çizer.
     */
    fun toUnavailable(city: City): CityWeatherUiModel = CityWeatherUiModel(
        cityId = city.id,
        name = city.name,
        region = city.region,
        country = city.country,
        weather = CardWeather.Unavailable,
        isLocation = city.isLocation
    )
    fun toForecastDetailUiModel(forecast: Forecast, unit: TemperatureUnit = TemperatureUnit.CELSIUS): ForecastDetailUiModel =
        ForecastDetailUiModel(
            city = CityUiModel(
                id = forecast.city.id,
                name = forecast.city.name,
                region = forecast.city.region,
                country = forecast.city.country,
                latitude = forecast.city.coordinates.latitude,
                longitude = forecast.city.coordinates.longitude
            ),
            currentWeather = CurrentWeatherUiModel(
                temperatureText = formatTemperature(forecast.currentWeather.temperatureCelsius, unit),
                conditionEmoji = emojiFor(forecast.currentWeather.condition),
                conditionLabel = labelFor(forecast.currentWeather.condition),
                windSpeedText = "${forecast.currentWeather.windSpeedKmh} km/s",
                humidityText = "%${forecast.currentWeather.humidityPercent}"
            ),
            hourlyForecast = forecast.hourlyForecast.map { hourly ->
                HourlyForecastUiModel(
                    timeText = formatHour(hourly.hourOfDay),
                    temperatureText = formatTemperature(hourly.temperatureCelsius, unit),
                    conditionEmoji = emojiFor(hourly.condition),
                    precipitationProbabilityText = "%${hourly.precipitationProbabilityPercent}"
                )
            },
            dailyForecast = forecast.dailyForecast.map { daily ->
                DailyForecastUiModel(
                    dayOffset = daily.dayOffset,
                    dateText = if (daily.dayOffset == 0) {
                        UiText.Resource(R.string.forecast_today)
                    } else {
                        UiText.Resource(R.string.forecast_day_offset, daily.dayOffset)
                    },
                    minTemperatureText = formatTemperature(daily.minTemperatureCelsius, unit),
                    maxTemperatureText = formatTemperature(daily.maxTemperatureCelsius, unit),
                    conditionEmoji = emojiFor(daily.condition)
                )
            }
        )

    private fun formatTemperature(celsius: Int, unit: TemperatureUnit): String = when (unit) {
        TemperatureUnit.CELSIUS -> "$celsius°C"
        TemperatureUnit.FAHRENHEIT -> "${celsius * 9 / 5 + 32}°F"
    }

    private fun formatHour(hour: Int): String = "%02d:00".format(hour)

    private fun emojiFor(condition: WeatherCondition): String = when (condition) {
        WeatherCondition.CLEAR -> "☀️"
        WeatherCondition.PARTLY_CLOUDY -> "🌤️"
        WeatherCondition.CLOUDY -> "☁️"
        WeatherCondition.RAIN -> "🌧️"
        WeatherCondition.SNOW -> "❄️"
        WeatherCondition.THUNDERSTORM -> "⛈️"
    }

    private fun labelFor(condition: WeatherCondition): UiText = when (condition) {
        WeatherCondition.CLEAR -> UiText.Resource(R.string.condition_clear)
        WeatherCondition.PARTLY_CLOUDY -> UiText.Resource(R.string.condition_partly_cloudy)
        WeatherCondition.CLOUDY -> UiText.Resource(R.string.condition_cloudy)
        WeatherCondition.RAIN -> UiText.Resource(R.string.condition_rain)
        WeatherCondition.SNOW -> UiText.Resource(R.string.condition_snow)
        WeatherCondition.THUNDERSTORM -> UiText.Resource(R.string.condition_thunderstorm)
    }
}

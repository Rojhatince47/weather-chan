package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import javax.inject.Inject

class WeatherUiMapper @Inject constructor() {

    fun toCityWeatherUiModel(cityWeather: CityWeather, isFavorite: Boolean): CityWeatherUiModel = CityWeatherUiModel(
        cityId = cityWeather.city.id,
        name = cityWeather.city.name,
        region = cityWeather.city.region,
        country = cityWeather.city.country,
        temperatureText = formatTemperature(cityWeather.currentWeather.temperatureCelsius),
        conditionEmoji = emojiFor(cityWeather.currentWeather.condition),
        conditionLabel = labelFor(cityWeather.currentWeather.condition),
        isFavorite = isFavorite
    )

    fun toForecastDetailUiModel(forecast: Forecast, isFavorite: Boolean): ForecastDetailUiModel = ForecastDetailUiModel(
        city = CityUiModel(
            id = forecast.city.id,
            name = forecast.city.name,
            region = forecast.city.region,
            country = forecast.city.country,
            latitude = forecast.city.coordinates.latitude,
            longitude = forecast.city.coordinates.longitude
        ),
        currentWeather = CurrentWeatherUiModel(
            temperatureText = formatTemperature(forecast.currentWeather.temperatureCelsius),
            conditionEmoji = emojiFor(forecast.currentWeather.condition),
            conditionLabel = labelFor(forecast.currentWeather.condition),
            windSpeedText = "${forecast.currentWeather.windSpeedKmh} km/s",
            humidityText = "%${forecast.currentWeather.humidityPercent}"
        ),
        hourlyForecast = forecast.hourlyForecast.map { hourly ->
            HourlyForecastUiModel(
                timeText = formatHour(hourly.hourOfDay),
                temperatureText = formatTemperature(hourly.temperatureCelsius),
                conditionEmoji = emojiFor(hourly.condition),
                precipitationProbabilityText = "%${hourly.precipitationProbabilityPercent}"
            )
        },
        dailyForecast = forecast.dailyForecast.map { daily ->
            DailyForecastUiModel(
                dateText = if (daily.dayOffset == 0) "Bugün" else "+${daily.dayOffset} gün",
                minTemperatureText = formatTemperature(daily.minTemperatureCelsius),
                maxTemperatureText = formatTemperature(daily.maxTemperatureCelsius),
                conditionEmoji = emojiFor(daily.condition)
            )
        },
        isFavorite = isFavorite
    )

    private fun formatTemperature(value: Int): String = "$value°C"

    private fun formatHour(hour: Int): String = "%02d:00".format(hour)

    private fun emojiFor(condition: WeatherCondition): String = when (condition) {
        WeatherCondition.CLEAR -> "☀️"
        WeatherCondition.PARTLY_CLOUDY -> "🌤️"
        WeatherCondition.CLOUDY -> "☁️"
        WeatherCondition.RAIN -> "🌧️"
        WeatherCondition.SNOW -> "❄️"
        WeatherCondition.THUNDERSTORM -> "⛈️"
    }

    private fun labelFor(condition: WeatherCondition): UiText = UiText.Dynamic(
        when (condition) {
            WeatherCondition.CLEAR -> "Açık"
            WeatherCondition.PARTLY_CLOUDY -> "Parçalı bulutlu"
            WeatherCondition.CLOUDY -> "Bulutlu"
            WeatherCondition.RAIN -> "Yağmurlu"
            WeatherCondition.SNOW -> "Kar yağışlı"
            WeatherCondition.THUNDERSTORM -> "Gök gürültülü"
        }
    )
}

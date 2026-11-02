package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition

data class ForecastDetailUiModel(
    val city: CityUiModel,
    val currentWeather: CurrentWeatherUiModel,
    val hourlyForecast: List<HourlyForecastUiModel>,
    val dailyForecast: List<DailyForecastUiModel>,
    val companion: CompanionUiModel
)

data class CityUiModel(
    val id: Long,
    val name: String,
    val region: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double
)

data class CurrentWeatherUiModel(
    val temperatureText: String,
    val icon: WeatherIcon,
    val conditionLabel: UiText,
    val windSpeedText: String,
    val humidityText: String,
    val condition: WeatherCondition,
    val isDay: Boolean
)

data class HourlyForecastUiModel(
    val timeLabel: UiText,
    val temperatureText: String,
    val temperatureValue: Int,
    val icon: WeatherIcon,
    val precipitationProbabilityText: String
)

data class DailyForecastUiModel(
    // Liste anahtarı olarak kullanılır: Bundle'da saklanabilir bir tür olmalı,
    // bu yüzden dateText (UiText) değil dayOffset kullanılır.
    val dayOffset: Int,
    val dateText: UiText,
    val minTemperatureText: String,
    val maxTemperatureText: String,
    val icon: WeatherIcon,
    val condition: WeatherCondition
)

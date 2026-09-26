package io.github.rojhatince47.weatherchan.feature.weather.domain.model

data class HourlyForecast(
    val hourOfDay: Int,
    val temperatureCelsius: Int,
    val condition: WeatherCondition,
    val precipitationProbabilityPercent: Int
)

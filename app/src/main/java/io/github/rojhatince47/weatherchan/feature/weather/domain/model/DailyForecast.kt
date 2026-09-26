package io.github.rojhatince47.weatherchan.feature.weather.domain.model

data class DailyForecast(
    val dayOffset: Int,
    val minTemperatureCelsius: Int,
    val maxTemperatureCelsius: Int,
    val condition: WeatherCondition
)

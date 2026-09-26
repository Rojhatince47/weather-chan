package io.github.rojhatince47.weatherchan.feature.weather.domain.model

data class CurrentWeather(
    val temperatureCelsius: Int,
    val condition: WeatherCondition,
    val windSpeedKmh: Int,
    val humidityPercent: Int
)

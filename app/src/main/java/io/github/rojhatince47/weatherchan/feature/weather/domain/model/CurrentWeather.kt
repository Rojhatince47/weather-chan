package io.github.rojhatince47.weatherchan.feature.weather.domain.model

data class CurrentWeather(
    val temperatureCelsius: Int,
    val condition: WeatherCondition,
    val windSpeedKmh: Int,
    val humidityPercent: Int,
    /** Gündüz mü (konumun gerçek güneş bilgisi). Default = mevcut testler değişmez. */
    val isDay: Boolean = true
)

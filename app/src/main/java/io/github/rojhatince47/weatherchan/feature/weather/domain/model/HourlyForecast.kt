package io.github.rojhatince47.weatherchan.feature.weather.domain.model

data class HourlyForecast(
    val hourOfDay: Int,
    val temperatureCelsius: Int,
    val condition: WeatherCondition,
    val precipitationProbabilityPercent: Int,
    /** Gün doğuş-batıştan türetilmiş saatlik gün bilgisi (veri yoksa 06-18). */
    val isDay: Boolean = true
)

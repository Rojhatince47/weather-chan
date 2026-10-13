package io.github.rojhatince47.weatherchan.feature.weather.domain.model

import java.time.LocalDateTime

data class HourlyForecast(
    /** Mutlak yerel zaman damgası — pencerelemenin tek dayanağı (spec K2). */
    val localDateTime: LocalDateTime,
    val hourOfDay: Int,
    val temperatureCelsius: Int,
    val condition: WeatherCondition,
    val precipitationProbabilityPercent: Int,
    /** Gün doğuş-batıştan türetilmiş saatlik gün bilgisi (veri yoksa 06-18). */
    val isDay: Boolean = true
)

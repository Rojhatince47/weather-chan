package io.github.rojhatince47.weatherchan.feature.weather.domain.model

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * "Şimdi + 24 saat" kayar penceresi (spec K1): [start] saat kesimlenir,
 * aralık iki uç dahil — saatlik hizalı veride 25 öğe. Saf; Clock/now görmez.
 */
fun hourlyWindow(hours: List<HourlyForecast>, start: LocalDateTime): List<HourlyForecast> {
    val s = start.truncatedTo(ChronoUnit.HOURS)
    return hours.filter { it.localDateTime.truncatedTo(ChronoUnit.HOURS) in s..s.plusHours(24) }
}

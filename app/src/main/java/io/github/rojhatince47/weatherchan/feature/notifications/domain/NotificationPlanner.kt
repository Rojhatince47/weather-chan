package io.github.rojhatince47.weatherchan.feature.notifications.domain

import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationPayload
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.PlannedNotification
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.PlannerInput
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.PlannerOutput
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.SevereReason
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceContext
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.HourlyOutlook
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.WeatherAdviceEngine
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Bildirim karar motoru: tamamen saf, Android bağımlılığı yoktur.
 *
 * Sıra güvenlikten konfora: şiddetli hava → yaklaşan yağmur → günlük özet →
 * karakter mesajı. Karakter mesajı yalnız o gün hiç bildirim atılmadıysa ve
 * bu turda başka bildirim çıkmadıysa gönderilir; böylece günlük tavan
 * (2 şiddetli + 1 yağmur + 1 özet) aşılmaz.
 *
 * Eşikler tavsiye motoruyla aynı ruhta tutulur; sessiz saat 23:00–08:00
 * aralığında yalnız şiddetli uyarı geçer.
 */
class NotificationPlanner @Inject constructor(
    private val adviceEngine: WeatherAdviceEngine
) {

    fun plan(input: PlannerInput): PlannerOutput {
        val history = input.history.forDay(input.localNow.toLocalDate())
        if (!input.settings.masterEnabled) return PlannerOutput(emptyList(), history)

        val planned = mutableListOf<PlannedNotification>()
        severe(input)?.let(planned::add)
        if (!isQuietHour(input.localNow)) {
            rainSoon(input)?.let(planned::add)
            summary(input)?.let(planned::add)
            // Karakter mesajı aynı turda başka bildirimle binmez ve o gün
            // şiddetli/yağmur uyarısından sonra gelmez. Günlük özet engel
            // değildir: özet sabah gider, karakter gün içinde tek başına.
            val alertsToday = history.countOf(NotificationType.SEVERE_WEATHER) +
                history.countOf(NotificationType.RAIN_SOON)
            if (planned.isEmpty() && alertsToday == 0) {
                companion(input)?.let(planned::add)
            }
        }

        val updated = planned.fold(history) { acc, notification ->
            acc.record(notification.type, input.now)
        }
        return PlannerOutput(planned, updated)
    }

    private fun severe(input: PlannerInput): PlannedNotification? {
        if (!input.settings.isEnabled(NotificationType.SEVERE_WEATHER)) return null
        if (input.history.countOf(NotificationType.SEVERE_WEATHER) >= MAX_SEVERE_PER_DAY) return null
        if (isCoolingDown(input, NotificationType.SEVERE_WEATHER)) return null

        val current = input.forecast.currentWeather
        val reason = when {
            current.condition == WeatherCondition.THUNDERSTORM -> SevereReason.STORM
            current.condition == WeatherCondition.SNOW -> SevereReason.SNOW
            current.windSpeedKmh >= SEVERE_WIND_KMH -> SevereReason.WIND
            current.temperatureCelsius <= SEVERE_COLD_C -> SevereReason.COLD
            current.temperatureCelsius >= SEVERE_HOT_C -> SevereReason.HOT
            else -> return null
        }
        return PlannedNotification(
            type = NotificationType.SEVERE_WEATHER,
            payload = NotificationPayload.Severe(reason)
        )
    }

    private fun rainSoon(input: PlannerInput): PlannedNotification? {
        if (!input.settings.isEnabled(NotificationType.RAIN_SOON)) return null
        if (input.history.countOf(NotificationType.RAIN_SOON) >= MAX_RAIN_PER_DAY) return null
        if (isCoolingDown(input, NotificationType.RAIN_SOON)) return null
        // Zaten yağıyorsa "birazdan başlayacak" denmez.
        if (input.forecast.currentWeather.condition.isWet()) return null

        val deadline = input.localNow.plusHours(RAIN_LOOKAHEAD_HOURS)
        val start = input.forecast.hourlyForecast
            .filter { it.localDateTime.isAfter(input.localNow) && !it.localDateTime.isAfter(deadline) }
            .filter {
                it.condition.isWet() &&
                    it.precipitationProbabilityPercent >= RAIN_SOON_THRESHOLD
            }
            .minByOrNull { it.localDateTime } ?: return null

        val startsWithin = ChronoUnit.HOURS
            .between(input.localNow, start.localDateTime)
            .toInt()
            .coerceIn(1, RAIN_LOOKAHEAD_HOURS.toInt())
        return PlannedNotification(
            type = NotificationType.RAIN_SOON,
            payload = NotificationPayload.RainSoon(startsWithin)
        )
    }

    private fun summary(input: PlannerInput): PlannedNotification? {
        if (!input.settings.isEnabled(NotificationType.DAILY_SUMMARY)) return null
        if (input.history.countOf(NotificationType.DAILY_SUMMARY) >= MAX_SUMMARY_PER_DAY) return null
        if (input.localNow.toLocalTime() < input.settings.summaryTime) return null

        val daily = input.forecast.dailyForecast.firstOrNull() ?: return null
        val today = input.localNow.toLocalDate()
        val rainChance = input.forecast.hourlyForecast
            .filter { it.localDateTime.toLocalDate() == today }
            .maxOfOrNull { it.precipitationProbabilityPercent } ?: 0
        return PlannedNotification(
            type = NotificationType.DAILY_SUMMARY,
            payload = NotificationPayload.Summary(daily, rainChance)
        )
    }

    private fun companion(input: PlannerInput): PlannedNotification? {
        if (!input.settings.isEnabled(NotificationType.COMPANION)) return null
        if (input.history.countOf(NotificationType.COMPANION) >= MAX_COMPANION_PER_DAY) return null
        if (input.localNow.hour !in COMPANION_HOURS) return null

        val advice = adviceEngine.advise(adviceContextOf(input))
        return PlannedNotification(
            type = NotificationType.COMPANION,
            payload = NotificationPayload.Companion(advice.category)
        )
    }

    private fun adviceContextOf(input: PlannerInput): AdviceContext {
        val current = input.forecast.currentWeather
        return AdviceContext(
            cityId = input.forecast.city.id,
            temperatureCelsius = current.temperatureCelsius,
            condition = current.condition,
            isDay = current.isDay,
            windSpeedKmh = current.windSpeedKmh,
            upcomingHours = input.forecast.hourlyForecast
                .filter { it.localDateTime.isAfter(input.localNow) }
                .take(RAIN_LOOKAHEAD_HOURS.toInt())
                .map { HourlyOutlook(it.precipitationProbabilityPercent, it.condition) },
            epochDay = input.localNow.toLocalDate().toEpochDay(),
            hourOfDay = input.localNow.hour
        )
    }

    private fun isCoolingDown(input: PlannerInput, type: NotificationType): Boolean {
        val last = input.history.lastSentOf(type) ?: return false
        return Duration.between(last, input.now) < COOLDOWN
    }

    /** 23:00–08:00 aralığı sessizdir; yalnız şiddetli uyarı geçer. */
    private fun isQuietHour(localNow: LocalDateTime): Boolean = localNow.hour >= QUIET_START_HOUR || localNow.hour < QUIET_END_HOUR

    private fun WeatherCondition.isWet(): Boolean = when (this) {
        WeatherCondition.RAIN,
        WeatherCondition.LIGHT_SHOWERS,
        WeatherCondition.SNOW,
        WeatherCondition.THUNDERSTORM -> true
        else -> false
    }

    private companion object {
        const val MAX_SEVERE_PER_DAY = 2
        const val MAX_RAIN_PER_DAY = 1
        const val MAX_SUMMARY_PER_DAY = 1
        const val MAX_COMPANION_PER_DAY = 1

        const val RAIN_SOON_THRESHOLD = 50
        const val SEVERE_WIND_KMH = 60
        const val SEVERE_COLD_C = -10
        const val SEVERE_HOT_C = 40

        const val QUIET_START_HOUR = 23
        const val QUIET_END_HOUR = 8
        val COMPANION_HOURS = 10..20

        const val RAIN_LOOKAHEAD_HOURS = 2L
        val COOLDOWN: Duration = Duration.ofHours(6)
    }
}

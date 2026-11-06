package io.github.rojhatince47.weatherchan.feature.notifications.domain.model

import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceCategory
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import java.time.Instant
import java.time.LocalDateTime

/** Şiddetli havanın alt nedeni; metin seçimi buna göre yapılır. */
enum class SevereReason {
    STORM,
    SNOW,
    WIND,
    COLD,
    HOT
}

sealed interface NotificationPayload {
    data class Severe(val reason: SevereReason) : NotificationPayload

    data object RainSoon : NotificationPayload

    data class Summary(
        val daily: DailyForecast,
        val rainChancePercent: Int,
        /** Günün kalanında en yüksek ihtimalin saati; yoksa null. */
        val peakHour: Int? = null
    ) : NotificationPayload

    data class Companion(
        val category: AdviceCategory,
        val variantSeed: Int = 0
    ) : NotificationPayload
}

data class PlannedNotification(
    val type: NotificationType,
    val payload: NotificationPayload
)

/**
 * Karar motorunun saf girdisi.
 *
 * [now] süre hesabı için mutlak an, [localNow] sessiz saat/özet saati için
 * cihazın yerel saatidir. İkisi ayrı tutulur ki testler saat diliminden
 * bağımsız kalsın.
 */
data class PlannerInput(
    val now: Instant,
    val localNow: LocalDateTime,
    /** Şehrin yerel saati; saatlik damgalar bu dilimdedir. */
    val cityNow: LocalDateTime = localNow,
    val settings: NotificationSettings,
    val history: NotificationHistory,
    val forecast: Forecast
)

data class PlannerOutput(
    val notifications: List<PlannedNotification>,
    val history: NotificationHistory
)

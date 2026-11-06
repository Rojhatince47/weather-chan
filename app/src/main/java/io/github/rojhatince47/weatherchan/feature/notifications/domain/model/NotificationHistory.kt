package io.github.rojhatince47.weatherchan.feature.notifications.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Bugünün bildirim geçmişi. İşçi her kontrolde okur, karar motoru güncellenmiş
 * halini döndürür; diskte tutulur ki süreç ölse bile tavanlar korunsun.
 */
data class NotificationHistory(
    val day: LocalDate,
    val sentCounts: Map<NotificationType, Int> = emptyMap(),
    val lastSentAt: Map<NotificationType, Instant> = emptyMap()
) {
    fun countOf(type: NotificationType): Int = sentCounts[type] ?: 0

    fun lastSentOf(type: NotificationType): Instant? = lastSentAt[type]

    fun totalToday(): Int = sentCounts.values.sum()

    /** Gün değiştiyse sayaçlar sıfırdan başlar. */
    fun forDay(day: LocalDate): NotificationHistory = if (this.day == day) this else NotificationHistory(day)

    fun record(type: NotificationType, at: Instant): NotificationHistory = copy(
        sentCounts = sentCounts + (type to countOf(type) + 1),
        lastSentAt = lastSentAt + (type to at)
    )
}

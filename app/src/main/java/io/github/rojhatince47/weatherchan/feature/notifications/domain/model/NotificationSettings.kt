package io.github.rojhatince47.weatherchan.feature.notifications.domain.model

import java.time.LocalTime

/**
 * Bildirim tercihleri. Varsayılan **hepsi açık**: kullanıcı isterse kapatır.
 *
 * [summaryTime] cihazın yerel saatidir. Sessiz saat (23:00–08:00) içine
 * denk gelen bir seçim 08:00'e ertelenir; karar motoru bunu kendisi
 * normalleştirir, arayüz ayrıca kısıtlamaz.
 */
data class NotificationSettings(
    val masterEnabled: Boolean = true,
    val typeEnabled: Map<NotificationType, Boolean> = NotificationType.entries.associateWith { true },
    val summaryTime: LocalTime = DEFAULT_SUMMARY_TIME
) {
    fun isEnabled(type: NotificationType): Boolean = masterEnabled && (typeEnabled[type] ?: true)

    companion object {
        val DEFAULT_SUMMARY_TIME: LocalTime = LocalTime.of(8, 0)
    }
}

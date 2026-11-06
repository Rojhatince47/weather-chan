package io.github.rojhatince47.weatherchan.feature.notifications.domain.repository

import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationHistory
import java.time.LocalDate

/** Diskteki bildirim geçmişi; işçi süreçleri arasında tavanları korur. */
interface NotificationHistoryStore {
    suspend fun read(today: LocalDate): NotificationHistory

    suspend fun write(history: NotificationHistory)
}

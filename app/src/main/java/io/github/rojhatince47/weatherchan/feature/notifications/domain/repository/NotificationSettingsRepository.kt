package io.github.rojhatince47.weatherchan.feature.notifications.domain.repository

import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import java.time.LocalTime
import kotlinx.coroutines.flow.StateFlow

interface NotificationSettingsRepository {
    val settings: StateFlow<NotificationSettings>

    /** Diskteki güncel değeri bekleyerek okur (işçi kullanır). */
    suspend fun read(): NotificationSettings

    suspend fun setMasterEnabled(enabled: Boolean)

    suspend fun setTypeEnabled(type: NotificationType, enabled: Boolean)

    suspend fun setSummaryTime(time: LocalTime)
}

package io.github.rojhatince47.weatherchan.feature.notifications.data.repository

import io.github.rojhatince47.weatherchan.core.common.dispatcher.IoDispatcher
import io.github.rojhatince47.weatherchan.feature.notifications.data.local.NotificationPreferences
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationSettingsRepository
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

@Singleton
class DataStoreNotificationSettingsRepository @Inject constructor(
    private val preferences: NotificationPreferences,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : NotificationSettingsRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override val settings: StateFlow<NotificationSettings> = preferences.settings()
        .stateIn(scope, SharingStarted.Eagerly, NotificationSettings())

    override suspend fun read(): NotificationSettings = preferences.settings().first()

    override suspend fun setMasterEnabled(enabled: Boolean) {
        preferences.writeMasterEnabled(enabled)
    }

    override suspend fun setTypeEnabled(type: NotificationType, enabled: Boolean) {
        preferences.writeTypeEnabled(type, enabled)
    }

    override suspend fun setSummaryTime(time: LocalTime) {
        preferences.writeSummaryTime(time)
    }
}

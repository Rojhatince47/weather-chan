package io.github.rojhatince47.weatherchan.feature.notifications.testing

import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationSettingsRepository
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeNotificationSettingsRepository(
    initial: NotificationSettings = NotificationSettings()
) : NotificationSettingsRepository {

    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<NotificationSettings> = state.asStateFlow()

    override suspend fun setMasterEnabled(enabled: Boolean) {
        state.value = state.value.copy(masterEnabled = enabled)
    }

    override suspend fun setTypeEnabled(type: NotificationType, enabled: Boolean) {
        state.value = state.value.copy(
            typeEnabled = state.value.typeEnabled + (type to enabled)
        )
    }

    override suspend fun setSummaryTime(time: LocalTime) {
        state.value = state.value.copy(summaryTime = time)
    }
}

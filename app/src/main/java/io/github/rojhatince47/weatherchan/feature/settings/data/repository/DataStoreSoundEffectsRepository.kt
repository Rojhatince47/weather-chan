package io.github.rojhatince47.weatherchan.feature.settings.data.repository

import io.github.rojhatince47.weatherchan.core.common.dispatcher.IoDispatcher
import io.github.rojhatince47.weatherchan.feature.settings.data.local.SettingsPreferences
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.SoundEffectsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@Singleton
class DataStoreSoundEffectsRepository @Inject constructor(
    private val preferences: SettingsPreferences,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : SoundEffectsRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override val enabled: StateFlow<Boolean> = preferences.soundEffectsEnabled()
        .stateIn(scope, SharingStarted.Eagerly, true)

    override suspend fun setEnabled(enabled: Boolean) {
        preferences.writeSoundEffectsEnabled(enabled)
    }
}

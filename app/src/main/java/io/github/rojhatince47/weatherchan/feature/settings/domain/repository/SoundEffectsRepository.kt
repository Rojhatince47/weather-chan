package io.github.rojhatince47.weatherchan.feature.settings.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Weather-chan dokunuş seslerinin açık/kapalı tercihi. */
interface SoundEffectsRepository {
    val enabled: StateFlow<Boolean>

    suspend fun setEnabled(enabled: Boolean)
}

package io.github.rojhatince47.weatherchan.feature.settings.testing

import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.SoundEffectsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSoundEffectsRepository(initial: Boolean = true) : SoundEffectsRepository {

    private val state = MutableStateFlow(initial)
    override val enabled: StateFlow<Boolean> = state.asStateFlow()

    override suspend fun setEnabled(enabled: Boolean) {
        state.value = enabled
    }
}

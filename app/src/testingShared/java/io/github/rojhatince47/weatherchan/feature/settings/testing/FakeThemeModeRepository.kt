package io.github.rojhatince47.weatherchan.feature.settings.testing

import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.ThemeModeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeThemeModeRepository(initial: ThemeMode = ThemeMode.SYSTEM) : ThemeModeRepository {

    private val state = MutableStateFlow(initial)
    override val themeMode: StateFlow<ThemeMode> = state.asStateFlow()

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        state.value = themeMode
    }
}

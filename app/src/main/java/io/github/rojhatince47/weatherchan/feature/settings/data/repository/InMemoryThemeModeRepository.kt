package io.github.rojhatince47.weatherchan.feature.settings.data.repository

import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.ThemeModeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class InMemoryThemeModeRepository @Inject constructor() : ThemeModeRepository {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override fun setThemeMode(themeMode: ThemeMode) {
        _themeMode.value = themeMode
    }
}

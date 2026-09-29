package io.github.rojhatince47.weatherchan.feature.settings.domain.repository

import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface ThemeModeRepository {
    val themeMode: StateFlow<ThemeMode>

    suspend fun setThemeMode(themeMode: ThemeMode)
}

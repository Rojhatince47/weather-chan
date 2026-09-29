package io.github.rojhatince47.weatherchan.feature.settings.data.repository

import io.github.rojhatince47.weatherchan.core.common.dispatcher.IoDispatcher
import io.github.rojhatince47.weatherchan.feature.settings.data.local.SettingsPreferences
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.ThemeModeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@Singleton
class DataStoreThemeModeRepository @Inject constructor(
    private val preferences: SettingsPreferences,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : ThemeModeRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override val themeMode: StateFlow<ThemeMode> = preferences.themeMode()
        .stateIn(scope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        preferences.writeThemeMode(themeMode)
    }
}

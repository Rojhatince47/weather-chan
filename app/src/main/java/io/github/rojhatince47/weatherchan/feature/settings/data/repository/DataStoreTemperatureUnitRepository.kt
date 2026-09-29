package io.github.rojhatince47.weatherchan.feature.settings.data.repository

import io.github.rojhatince47.weatherchan.core.common.dispatcher.IoDispatcher
import io.github.rojhatince47.weatherchan.feature.settings.data.local.SettingsPreferences
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@Singleton
class DataStoreTemperatureUnitRepository @Inject constructor(
    private val preferences: SettingsPreferences,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : TemperatureUnitRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override val unit: StateFlow<TemperatureUnit> = preferences.unit()
        .stateIn(scope, SharingStarted.Eagerly, TemperatureUnit.CELSIUS)

    override suspend fun setUnit(unit: TemperatureUnit) {
        preferences.writeUnit(unit)
    }
}

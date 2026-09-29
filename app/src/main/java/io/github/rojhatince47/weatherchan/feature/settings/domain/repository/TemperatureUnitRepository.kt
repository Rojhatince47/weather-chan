package io.github.rojhatince47.weatherchan.feature.settings.domain.repository

import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import kotlinx.coroutines.flow.StateFlow

interface TemperatureUnitRepository {
    val unit: StateFlow<TemperatureUnit>

    suspend fun setUnit(unit: TemperatureUnit)
}

package io.github.rojhatince47.weatherchan.feature.settings.data.repository

import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class InMemoryTemperatureUnitRepository @Inject constructor() : TemperatureUnitRepository {

    private val _unit = MutableStateFlow(TemperatureUnit.CELSIUS)
    override val unit: StateFlow<TemperatureUnit> = _unit.asStateFlow()

    override fun setUnit(unit: TemperatureUnit) {
        _unit.value = unit
    }
}

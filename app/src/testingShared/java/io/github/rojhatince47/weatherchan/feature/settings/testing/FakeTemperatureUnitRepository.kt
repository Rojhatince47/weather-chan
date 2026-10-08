package io.github.rojhatince47.weatherchan.feature.settings.testing

import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeTemperatureUnitRepository(
    initial: TemperatureUnit = TemperatureUnit.CELSIUS
) : TemperatureUnitRepository {

    private val state = MutableStateFlow(initial)
    override val unit: StateFlow<TemperatureUnit> = state.asStateFlow()

    override suspend fun setUnit(unit: TemperatureUnit) {
        state.value = unit
    }
}

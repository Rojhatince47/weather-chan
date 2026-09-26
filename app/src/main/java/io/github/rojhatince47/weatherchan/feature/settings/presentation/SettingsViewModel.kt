package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.TemperatureUnitUiModel
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: TemperatureUnitRepository
) : ViewModel() {

    val uiState: StateFlow<UiState<TemperatureUnitUiModel>> = repository.unit
        .map<TemperatureUnit, UiState<TemperatureUnitUiModel>> { unit ->
            UiState.Success(unit.toUiModel())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UiState.Success(repository.unit.value.toUiModel())
        )

    fun onToggleUnit() {
        val next = when (repository.unit.value) {
            TemperatureUnit.CELSIUS -> TemperatureUnit.FAHRENHEIT
            TemperatureUnit.FAHRENHEIT -> TemperatureUnit.CELSIUS
        }
        repository.setUnit(next)
    }

    fun onUnitSelected(unit: TemperatureUnit) {
        repository.setUnit(unit)
    }
}

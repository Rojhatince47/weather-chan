package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.lifecycle.ViewModel
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.TemperatureUnitUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState: MutableStateFlow<UiState<TemperatureUnitUiModel>> =
        MutableStateFlow(UiState.Success(toUiModel(TemperatureUnit.CELSIUS)))

    val uiState: StateFlow<UiState<TemperatureUnitUiModel>> = _uiState.asStateFlow()

    fun onToggleUnit() {
        val current = (_uiState.value as? UiState.Success)?.data?.unit
            ?: TemperatureUnit.CELSIUS
        val next = when (current) {
            TemperatureUnit.CELSIUS -> TemperatureUnit.FAHRENHEIT
            TemperatureUnit.FAHRENHEIT -> TemperatureUnit.CELSIUS
        }
        _uiState.update { UiState.Success(toUiModel(next)) }
    }

    fun onUnitSelected(unit: TemperatureUnit) {
        _uiState.update { UiState.Success(toUiModel(unit)) }
    }

    private fun toUiModel(unit: TemperatureUnit): TemperatureUnitUiModel = TemperatureUnitUiModel(
        unit = unit,
        label = when (unit) {
            TemperatureUnit.CELSIUS -> UiText.Dynamic("Santigrat (°C)")
            TemperatureUnit.FAHRENHEIT -> UiText.Dynamic("Fahrenayt (°F)")
        }
    )
}

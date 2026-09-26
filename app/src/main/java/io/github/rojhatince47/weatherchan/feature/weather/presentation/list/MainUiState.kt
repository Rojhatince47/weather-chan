package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.TemperatureUnitUiModel
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel

data class MainUiState(
    val query: String = "",
    val content: UiState<List<CityWeatherUiModel>> = UiState.Loading,
    val unit: TemperatureUnitUiModel = TemperatureUnit.CELSIUS.toUiModel()
)

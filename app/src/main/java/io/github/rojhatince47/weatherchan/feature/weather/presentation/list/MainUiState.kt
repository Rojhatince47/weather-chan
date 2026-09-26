package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel

data class MainUiState(
    val query: String = "",
    val showFavoritesOnly: Boolean = false,
    val content: UiState<List<CityWeatherUiModel>> = UiState.Loading,
    val isRefreshing: Boolean = false
)

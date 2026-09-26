package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.ForecastDetailUiModel

data class DetailUiState(
    val content: UiState<ForecastDetailUiModel> = UiState.Loading
)

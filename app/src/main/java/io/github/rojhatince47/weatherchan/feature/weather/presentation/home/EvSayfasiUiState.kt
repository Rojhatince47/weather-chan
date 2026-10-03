package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.presentation.detail.DetailUiState

data class EvSayfasiUiState(
    val pages: List<City> = emptyList(),
    val currentPage: Int = 0,
    val forecasts: Map<Long, DetailUiState> = emptyMap()
)

package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.presentation.detail.DetailUiState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationPermissionState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState

data class EvSayfasiUiState(
    val pages: List<City> = emptyList(),
    val currentPage: Int = 0,
    val forecasts: Map<Long, DetailUiState> = emptyMap(),
    val permissionState: LocationPermissionState = LocationPermissionState.Restoring,
    val locationState: LocationState = LocationState.Idle
)

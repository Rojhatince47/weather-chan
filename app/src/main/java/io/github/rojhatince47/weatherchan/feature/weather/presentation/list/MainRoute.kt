package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City

@Composable
fun MainRoute(onCityClick: (City) -> Unit, modifier: Modifier = Modifier, viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onCityClick = { cityId -> viewModel.findCity(cityId)?.let(onCityClick) },
        onFavoriteClick = viewModel::onFavoriteClick,
        onRetry = viewModel::onRetry,
        onRefresh = viewModel::onRefresh,
        modifier = modifier
    )
}

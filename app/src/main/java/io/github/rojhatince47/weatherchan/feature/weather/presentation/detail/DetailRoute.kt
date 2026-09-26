package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DetailRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: DetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DetailScreen(
        uiState = uiState,
        onBack = onBack,
        onFavoriteClick = viewModel::onFavoriteClick,
        onRetry = viewModel::onRetry,
        onRefresh = viewModel::onRefresh,
        modifier = modifier
    )
}

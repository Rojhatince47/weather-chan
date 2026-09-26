package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City

@Composable
fun MainRoute(
    onCityClick: (City) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    // NavHost üstünde alınan ortak örnek; giriş başına ayrı örnek oluşmaz.
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val unit = (settingsState as? UiState.Success)?.data
        ?: TemperatureUnit.CELSIUS.toUiModel()

    MainScreen(
        uiState = uiState,
        unit = unit,
        onQueryChange = viewModel::onQueryChange,
        onCityClick = { cityId -> viewModel.findCity(cityId)?.let(onCityClick) },
        onFavoriteClick = viewModel::onFavoriteClick,
        onRetry = viewModel::onRetry,
        onRefresh = viewModel::onRefresh,
        onToggleUnit = settingsViewModel::onToggleUnit,
        modifier = modifier
    )
}

package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City

@Composable
fun AddCityRoute(
    onCityClick: (City) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddCityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AddCityScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onAddCity = viewModel::onAddCity,
        onOpenCity = { city ->
            // Büyüteç "göster" sözü verir: listeye eklenmemiş şehir önce
            // depoya yazılır (ekleme idempotent'tir), ardından pager'da açılır.
            viewModel.onAddCity(city)
            onCityClick(city)
        },
        onBack = onBack,
        modifier = modifier
    )
}

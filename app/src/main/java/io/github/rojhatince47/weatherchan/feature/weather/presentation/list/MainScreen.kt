package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.core.ui.component.HomeHeader
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.TemperatureUnitUiModel
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel

@Composable
fun MainScreen(
    uiState: MainUiState,
    unit: TemperatureUnitUiModel,
    onQueryChange: (String) -> Unit,
    onCityClick: (Long) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onToggleUnit: () -> Unit,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    // rememberSaveable: detay ekranından geri dönüldüğünde liste
    // kaydırma konumu korunur.
    listState: LazyListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }
) {
    Column(modifier = modifier.fillMaxSize()) {
        HomeHeader(
            title = "Weather-chan",
            description = "Şehirlerin havasını sevimli takip et"
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = uiState.showFavoritesOnly,
                onClick = { onFavoritesOnlyChange(!uiState.showFavoritesOnly) },
                label = { Text(text = "Favoriler") }
            )
            TextButton(onClick = onToggleUnit) {
                Text(
                    text = if (unit.unit == TemperatureUnit.CELSIUS) {
                        "°C"
                    } else {
                        "°F"
                    }
                )
            }
        }
        CitySearchField(
            value = uiState.query,
            onValueChange = onQueryChange
        )
        when (val content = uiState.content) {
            is UiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.padding(24.dp)
            )
            is UiState.Empty -> Text(
                text = "Sonuç bulunamadı",
                modifier = Modifier.padding(24.dp)
            )
            is UiState.Error -> Text(
                text = content.message.asString(),
                modifier = Modifier.padding(24.dp)
            )
            is UiState.Success -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(content.data, key = { it.cityId }) { cityWeather ->
                    CityWeatherCard(
                        cityWeather = cityWeather,
                        onClick = { onCityClick(cityWeather.cityId) },
                        onFavoriteClick = { onFavoriteClick(cityWeather.cityId) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun MainScreenCompactPreview() {
    HavaTheme {
        MainScreen(
            uiState = sampleMainUiState(),
            unit = TemperatureUnit.CELSIUS.toUiModel(),
            onQueryChange = {},
            onCityClick = {},
            onFavoriteClick = {},
            onRetry = {},
            onRefresh = {},
            onToggleUnit = {},
            onFavoritesOnlyChange = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 600)
@Composable
private fun MainScreenMediumPreview() {
    HavaTheme {
        MainScreen(
            uiState = sampleMainUiState(),
            unit = TemperatureUnit.CELSIUS.toUiModel(),
            onQueryChange = {},
            onCityClick = {},
            onFavoriteClick = {},
            onRetry = {},
            onRefresh = {},
            onToggleUnit = {},
            onFavoritesOnlyChange = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 840)
@Composable
private fun MainScreenExpandedPreview() {
    HavaTheme {
        MainScreen(
            uiState = sampleMainUiState(),
            unit = TemperatureUnit.CELSIUS.toUiModel(),
            onQueryChange = {},
            onCityClick = {},
            onFavoriteClick = {},
            onRetry = {},
            onRefresh = {},
            onToggleUnit = {},
            onFavoritesOnlyChange = {}
        )
    }
}

private fun sampleMainUiState(): MainUiState = MainUiState(
    content = UiState.Success(
        listOf(
            CityWeatherUiModel(
                cityId = 1L,
                name = "İstanbul",
                region = "Marmara",
                country = "Türkiye",
                temperatureText = "21°C",
                conditionEmoji = "☀️",
                conditionLabel = UiText.Dynamic("Açık"),
                isFavorite = false
            )
        )
    )
)

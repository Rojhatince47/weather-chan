package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.core.ui.component.HomeHeader
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel

@Composable
fun MainScreen(
    uiState: MainUiState,
    onQueryChange: (String) -> Unit,
    onCityClick: (Long) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        HomeHeader(
            title = "Weather-chan",
            description = "Şehirlerin havasını sevimli takip et"
        )
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

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    HavaTheme {
        MainScreen(
            uiState = MainUiState(
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
            ),
            onQueryChange = {},
            onCityClick = {},
            onFavoriteClick = {},
            onRetry = {},
            onRefresh = {}
        )
    }
}

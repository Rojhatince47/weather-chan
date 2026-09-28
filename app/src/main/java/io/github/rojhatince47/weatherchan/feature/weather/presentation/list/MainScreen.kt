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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.component.HomeHeader
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.text.toUiText
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.core.ui.theme.ThemeModeIcon
import io.github.rojhatince47.weatherchan.core.ui.theme.resolveIsDark
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.TemperatureUnitUiModel
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel

@Composable
fun MainScreen(
    uiState: MainUiState,
    unit: TemperatureUnitUiModel,
    themeMode: ThemeMode,
    onQueryChange: (String) -> Unit,
    onCityClick: (Long) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    onAddCity: (City) -> Unit,
    onOpenCity: (City) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onToggleUnit: () -> Unit,
    onToggleThemeMode: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    // rememberSaveable: detay ekranından geri dönüldüğünde liste
    // kaydırma konumu korunur.
    listState: LazyListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Kartların koşul paleti seçimi de aynı tema kararına bağlı olmalı;
            // sistem temasını doğrudan okumak kullanıcının seçimini yok sayardı.
            val isDarkTheme = themeMode.resolveIsDark()
            val themeModeName = stringResource(
                when (themeMode) {
                    ThemeMode.SYSTEM -> R.string.theme_mode_system
                    ThemeMode.LIGHT -> R.string.theme_mode_light
                    ThemeMode.DARK -> R.string.theme_mode_dark
                }
            )
            val themeLabel = stringResource(R.string.theme_toggle, themeModeName)

            HomeHeader(
                title = stringResource(R.string.home_title),
                description = stringResource(R.string.home_description)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = HavaSpacing.lg),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleThemeMode,
                    modifier = Modifier
                        .testTag("theme_toggle")
                        // ThemeModeIcon bir Canvas olduğu için erişilebilirlik
                        // etiketi düğmenin kendisine verilir. semantics bloğu
                        // composable olmadığından metin burada önceden çözülür.
                        .semantics { contentDescription = themeLabel }
                ) {
                    ThemeModeIcon(themeMode = themeMode)
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.action_refresh)
                    )
                }
                TextButton(onClick = onToggleUnit) {
                    Text(
                        text = if (unit.unit == TemperatureUnit.CELSIUS) {
                            stringResource(R.string.unit_celsius)
                        } else {
                            stringResource(R.string.unit_fahrenheit)
                        }
                    )
                }
            }
            CitySearchField(
                value = uiState.query,
                onValueChange = onQueryChange
            )
            // Canlı arama göstergeleri ince ve sabittir; sonuç satırları ise
            // kaydırılabilir listenin başına gömülür — 10 sonuç yerel listeyi
            // ekran dışına itmesin diye sabit bölüm kullanılmaz.
            when (val search = uiState.searchState) {
                SearchUiState.Idle -> Unit
                SearchUiState.Loading -> LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().testTag("search_indicator")
                )
                is SearchUiState.Failure -> Text(
                    text = search.error.toUiText().asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(
                        horizontal = HavaSpacing.lg,
                        vertical = HavaSpacing.sm
                    )
                )
                is SearchUiState.Success ->
                    if (uiState.content !is UiState.Success && search.results.isNotEmpty()) {
                        LiveSearchResults(
                            results = search.results,
                            onAddCity = onAddCity,
                            onOpenCity = onOpenCity
                        )
                    }
            }
            // Yenileme sırasında mevcut içerik korunur; yalnızca ince
            // ilerleme çubuğu gösterilir. Tam ekran Loading sadece ilk
            // yüklemede (içerik yokken) gösterilir.
            if (uiState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().testTag("refresh_indicator")
                )
            }
            when (val content = uiState.content) {
                is UiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.padding(HavaSpacing.xl)
                )
                is UiState.Empty -> Text(
                    text = stringResource(R.string.state_empty_list),
                    modifier = Modifier.padding(HavaSpacing.xl)
                )
                is UiState.Error -> Text(
                    text = content.message.asString(),
                    modifier = Modifier.padding(HavaSpacing.xl)
                )
                is UiState.Success -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(HavaSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(HavaSpacing.md)
                ) {
                    val search = uiState.searchState
                    if (search is SearchUiState.Success && search.results.isNotEmpty()) {
                        item(key = "live_search_results") {
                            LiveSearchResults(
                                results = search.results,
                                onAddCity = onAddCity,
                                onOpenCity = onOpenCity
                            )
                        }
                    }
                    items(content.data, key = { it.cityId }) { cityWeather ->
                        CityWeatherCard(
                            cityWeather = cityWeather,
                            isDarkTheme = isDarkTheme,
                            onClick = { onCityClick(cityWeather.cityId) },
                            onFavoriteClick = { onFavoriteClick(cityWeather.cityId) }
                        )
                    }
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
            themeMode = ThemeMode.SYSTEM,
            onQueryChange = {},
            onCityClick = {},
            onFavoriteClick = {},
            onAddCity = {},
            onOpenCity = {},
            onRetry = {},
            onRefresh = {},
            onToggleUnit = {},
            onToggleThemeMode = {}
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
            themeMode = ThemeMode.SYSTEM,
            onQueryChange = {},
            onCityClick = {},
            onFavoriteClick = {},
            onAddCity = {},
            onOpenCity = {},
            onRetry = {},
            onRefresh = {},
            onToggleUnit = {},
            onToggleThemeMode = {}
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
            themeMode = ThemeMode.SYSTEM,
            onQueryChange = {},
            onCityClick = {},
            onFavoriteClick = {},
            onAddCity = {},
            onOpenCity = {},
            onRetry = {},
            onRefresh = {},
            onToggleUnit = {},
            onToggleThemeMode = {}
        )
    }
}

/** Canlı arama satırları. Hem sabit bölümde hem kaydırılabilir liste içinde kullanılır. */
@Composable
private fun LiveSearchResults(results: List<SearchResultUiModel>, onAddCity: (City) -> Unit, onOpenCity: (City) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = HavaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(HavaSpacing.sm)
    ) {
        results.forEach { result ->
            SearchResultRow(
                result = result,
                onAddClick = { onAddCity(result.city) },
                onOpenClick = { onOpenCity(result.city) }
            )
        }
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
                condition = WeatherCondition.CLEAR,
                conditionEmoji = "☀️",
                conditionLabel = UiText.Dynamic("Açık"),
                isFavorite = false
            )
        )
    )
)

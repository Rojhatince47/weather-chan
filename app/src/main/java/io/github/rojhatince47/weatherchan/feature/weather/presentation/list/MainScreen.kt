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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CardWeather
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    unit: TemperatureUnitUiModel,
    themeMode: ThemeMode,
    onQueryChange: (String) -> Unit,
    onCityClick: (Long) -> Unit,
    onAddCity: (City) -> Unit,
    onOpenCity: (City) -> Unit,
    onDeleteCity: (Long) -> Unit,
    onAddLocation: () -> Unit,
    onRetryLocation: () -> Unit,
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
    // Uzun basma ile onaya kalkışılan şehir (id + görünen ad). Sheet açıkken
    // dolu tutulur; onay/iptal ya da alan dışına dokunma ile sıfırlanır.
    var pendingDelete: Pair<Long, String>? by remember { mutableStateOf(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // Gösterge M3'ün kendi IndicatorBox'ıdır: sürükleme mesafesine göre
        // aşağı iner, yenilenirken döner. State'i Box yönetir; bu yüzden
        // göstergeye testTag eklenemiyor (göstergeyi cihazda doğrularız).
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.testTag("pull_refresh")
        ) {
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
                // Yenileme sırasında mevcut içerik korunur; göstergeyi
                // PullToRefreshBox üretir. Tam ekran Loading sadece ilk
                // yüklemede (içerik yokken) gösterilir.
                when (val content = uiState.content) {
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
                                onLongClick = {
                                    pendingDelete = cityWeather.cityId to cityWeather.name
                                }
                            )
                        }
                        // Konum kaydı silinmişse ya da hiç okunamamışsa geri
                        // eklenebilir. Liste doluyken de görünür olmalı: aksi
                        // halde kullanıcı konumu yalnız listeyi boşaltarak geri
                        // getirebilirdi.
                        if (uiState.locationState !is LocationState.Resolved) {
                            item(key = "add_location") {
                                AddLocationRow(
                                    locationState = uiState.locationState,
                                    onAddLocation = onAddLocation,
                                    onRetryLocation = onRetryLocation
                                )
                            }
                        }
                    }
                    // Liste yokken (yükleme/boş/hata) kaydırılabilir bir yüzey gerekir:
                    // aksi halde PullToRefreshBox aşma hareketini harcayamaz, çekme
                    // çalışmaz.
                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (content) {
                            is UiState.Loading -> CircularProgressIndicator(
                                modifier = Modifier.padding(HavaSpacing.xl)
                            )
                            // Arama kutusu iki iş yapar: yeni şehir arar ve
                            // kayıtlı listeyi de filtreler. Sorgu varken liste
                            // boşluğu "sonuç bulunamadı" demek değildir —
                            // kullanıcı yeni şehir arıyordur ve ekranın sahibi
                            // canlı arama bölümüdür. Liste mesajı yalnız gerçekten
                            // boş listede (sorgu yokken) gösterilir; aramanın kendi
                            // boşluğu ayrı metinle anlatılır.
                            is UiState.Empty -> {
                                val search = uiState.searchState
                                when {
                                    uiState.query.isBlank() -> LocationEmptyState(
                                        locationState = uiState.locationState,
                                        onAddLocation = onAddLocation,
                                        onRetryLocation = onRetryLocation
                                    )
                                    search is SearchUiState.Success && search.results.isEmpty() -> Text(
                                        text = stringResource(R.string.state_empty_search),
                                        modifier = Modifier.padding(HavaSpacing.xl)
                                    )
                                    else -> Unit
                                }
                            }
                            is UiState.Error -> Text(
                                text = content.message.asString(),
                                modifier = Modifier.padding(HavaSpacing.xl)
                            )
                            is UiState.Success -> Unit
                        }
                    }
                }
            }
        }

        pendingDelete?.let { (cityId, cityName) ->
            DeleteConfirmationBottomSheet(
                cityName = cityName,
                onConfirm = {
                    onDeleteCity(cityId)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null }
            )
        }
    }
}

/**
 * Liste boşken gösterilen konum eylemi.
 *
 * Konum okunamadıysa mesaj **nedenini** anlatır ve eylem "Tekrar dene" olur;
 * aksi halde "Konumumu ekle" gösterilir. Konum okunuyorsa kısa bir bekleme
 * metni çıkar. Hiçbir dalda arama kutusu kaldırılmaz: konum eklenemezse
 * kullanıcı şehir aramayla ilerleyebilmelidir.
 */
@Composable
private fun LocationEmptyState(locationState: LocationState, onAddLocation: () -> Unit, onRetryLocation: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HavaSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HavaSpacing.md)
    ) {
        when (locationState) {
            is LocationState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.padding(HavaSpacing.sm))
                Text(
                    text = stringResource(R.string.location_loading),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            is LocationState.Unavailable -> {
                Text(
                    text = stringResource(R.string.location_unavailable),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onRetryLocation) {
                    Text(text = stringResource(R.string.location_retry))
                }
            }
            else -> {
                Text(
                    text = stringResource(R.string.location_permission_rationale),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onAddLocation) {
                    Text(text = stringResource(R.string.location_add_action))
                }
            }
        }
    }
}

/**
 * Liste doluyken gösterilen konum geri ekleme satırı.
 *
 * Liste boşken eklenen eylem burada da görünür olmalıdır: aksi halde kullanıcı
 * konumu yalnız listeyi boşaltarak geri getirebilirdi.
 */
@Composable
private fun AddLocationRow(locationState: LocationState, onAddLocation: () -> Unit, onRetryLocation: () -> Unit) {
    val retry = locationState is LocationState.Unavailable
    Button(
        onClick = if (retry) onRetryLocation else onAddLocation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (retry) {
                stringResource(R.string.location_retry)
            } else {
                stringResource(R.string.location_add_action)
            }
        )
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
            onAddCity = {},
            onOpenCity = {},
            onDeleteCity = {},
            onRetry = {},
            onRefresh = {},
            onToggleUnit = {},
            onToggleThemeMode = {},
            onAddLocation = {},
            onRetryLocation = {}
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
            onAddCity = {},
            onOpenCity = {},
            onDeleteCity = {},
            onRetry = {},
            onRefresh = {},
            onToggleUnit = {},
            onToggleThemeMode = {},
            onAddLocation = {},
            onRetryLocation = {}
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
            onAddCity = {},
            onOpenCity = {},
            onDeleteCity = {},
            onRetry = {},
            onRefresh = {},
            onToggleUnit = {},
            onToggleThemeMode = {},
            onAddLocation = {},
            onRetryLocation = {}
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
                weather = CardWeather.Loaded(
                    temperatureText = "21°C",
                    condition = WeatherCondition.CLEAR,
                    conditionEmoji = "☀️",
                    conditionLabel = UiText.Dynamic("Açık")
                )
            )
        )
    )
)

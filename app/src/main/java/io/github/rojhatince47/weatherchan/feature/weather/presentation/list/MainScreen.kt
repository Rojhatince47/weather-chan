package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.tooling.preview.Preview
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.component.AddLocationRow
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.core.ui.theme.resolveIsDark
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CardWeather
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    themeMode: ThemeMode,
    onCityClick: (Long) -> Unit,
    onAddCityClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDeleteCity: (Long) -> Unit,
    onRetryCity: (Long) -> Unit = {},
    onAddLocation: () -> Unit,
    onRetryLocation: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
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

    // Kartların koşul paleti seçimi de aynı tema kararına bağlı olmalı;
    // sistem temasını doğrudan okumak kullanıcının seçimini yok sayardı.
    val isDarkTheme = themeMode.resolveIsDark()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Ana ekran yalnız hava listesidir: arama ve ayarlar kendi
            // ekranlarına taşındı, üst çubukta yalnız iki giriş kalır.
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(
                        onClick = onAddCityClick,
                        modifier = Modifier.testTag("action_add_city")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_open_add_city)
                        )
                    }
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("action_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_open_settings)
                        )
                    }
                }
            )
        }
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
                // Yenileme sırasında mevcut içerik korunur; göstergeyi
                // PullToRefreshBox üretir. Tam ekran Loading sadece ilk
                // yüklemede (içerik yokken) gösterilir.
                when (val content = uiState.content) {
                    is UiState.Success -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(HavaSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(HavaSpacing.md)
                    ) {
                        items(content.data, key = { it.cityId }) { cityWeather ->
                            CityWeatherCard(
                                cityWeather = cityWeather,
                                isDarkTheme = isDarkTheme,
                                onClick = { onCityClick(cityWeather.cityId) },
                                onLongClick = {
                                    pendingDelete = cityWeather.cityId to cityWeather.name
                                },
                                onRetryCity = { onRetryCity(cityWeather.cityId) },
                                // İlk verisi çekilmekte olan kart da gösterge
                                // çizer: hata metni, bitmemiş bir işi anlatmaz.
                                isRetrying = cityWeather.cityId in uiState.retryingCityIds ||
                                    cityWeather.cityId in uiState.loadingCityIds
                            )
                        }
                        // Konum kaydı silinmişse ya da hiç okunamamışsa geri
                        // eklenebilir. Liste doluyken de görünür olmalı: aksi
                        // halde kullanıcı konumu yalnız listeyi boşaltarak geri
                        // getirebilirdi.
                        //
                        // Görünürlük `locationState`'in kendisine bakılarak
                        // karar verilmez: o durum her açılışta `Idle`/`Loading`
                        // ile başlar ve yalnız o anki yenileme denemesi
                        // bitince `Resolved` olur. Depoda önceki oturumdan kalma
                        // konum kartı varken de o süre boyunca buton görünür
                        // yanlışlıkla (konum okuma + geocoding + yedek ağ turu
                        // yavaş ağda ~25 s sürüyor). Karar veren veridir:
                        // konum kartı ekrandaysa satırın gösterileceği bir
                        // durum yoktur.
                        val hasLocationCard = content.data.any { it.isLocation }
                        if (!hasLocationCard && uiState.locationState !is LocationState.Resolved) {
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
                            is UiState.Empty -> LocationEmptyState(
                                locationState = uiState.locationState,
                                onAddLocation = onAddLocation,
                                onRetryLocation = onRetryLocation
                            )
                            is UiState.Error -> {
                                Text(
                                    text = content.message.asString(),
                                    modifier = Modifier.padding(HavaSpacing.xl)
                                )
                                // Hata yalnız metinle bitmemeli: çek-yenile her
                                // durumda çalışır ama açık bir eylem, ekranın
                                // toparlanabilir olduğunu söyler.
                                Button(onClick = onRetry) {
                                    Text(text = stringResource(R.string.action_retry))
                                }
                            }
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
 * metni çıkar. Konum eklenemezse kullanıcı şehri üst çubuktaki + ile
 * arayarak ekleyebilir.
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

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun MainScreenCompactPreview() {
    HavaTheme {
        MainScreen(
            uiState = sampleMainUiState(),
            themeMode = ThemeMode.SYSTEM,
            onCityClick = {},
            onAddCityClick = {},
            onSettingsClick = {},
            onDeleteCity = {},
            onRetryCity = {},
            onRetry = {},
            onRefresh = {},
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
            themeMode = ThemeMode.SYSTEM,
            onCityClick = {},
            onAddCityClick = {},
            onSettingsClick = {},
            onDeleteCity = {},
            onRetryCity = {},
            onRetry = {},
            onRefresh = {},
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
            themeMode = ThemeMode.SYSTEM,
            onCityClick = {},
            onAddCityClick = {},
            onSettingsClick = {},
            onDeleteCity = {},
            onRetryCity = {},
            onRetry = {},
            onRefresh = {},
            onAddLocation = {},
            onRetryLocation = {}
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

package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    onBack: () -> Unit,
    onDeleteCity: (Long) -> Unit,
    onRetryCity: (Long) -> Unit = {},
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    locationState: LocationState,
    onAddLocation: () -> Unit,
    onRetryLocation: () -> Unit,
    onAddCityClick: () -> Unit,
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
            // İnce yönetim ekranı: ayarlar ana ekrandadır; ekleme boş listede
            // bu ekranda ve ana ekranda; burada yalnız geri ve başlık kalır.
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("action_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
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
                            is UiState.Empty -> {
                                Text(
                                    text = stringResource(R.string.home_empty_state),
                                    modifier = Modifier.padding(HavaSpacing.xl)
                                )
                                // Konum akışı boş listede de görünür: kullanıcı izni
                                // verdikten sonra ne olduğunu görmeli, başarısızlıkta
                                // tekrar deneyebilmeli.
                                when {
                                    locationState is LocationState.Loading -> Text(
                                        text = stringResource(R.string.location_loading),
                                        modifier = Modifier
                                            .padding(top = HavaSpacing.md)
                                            .testTag("location_loading")
                                    )
                                    locationState !is LocationState.Resolved -> AddLocationRow(
                                        locationState = locationState,
                                        onAddLocation = onAddLocation,
                                        onRetryLocation = onRetryLocation,
                                        modifier = Modifier
                                            .padding(top = HavaSpacing.md)
                                            .padding(horizontal = HavaSpacing.lg)
                                    )
                                }
                                Button(
                                    onClick = onAddCityClick,
                                    modifier = Modifier.padding(top = HavaSpacing.md)
                                ) {
                                    Text(text = stringResource(R.string.action_open_add_city))
                                }
                            }
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
                // Konumun adı boş olabilir; dil kaynağından çözülür.
                cityName = cityName.ifBlank { stringResource(R.string.location_default_name) },
                onConfirm = {
                    onDeleteCity(cityId)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null }
            )
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
            locationState = LocationState.Idle,
            onAddLocation = {},
            onRetryLocation = {},
            onAddCityClick = {},
            onCityClick = {},
            onBack = {},
            onDeleteCity = {},
            onRetryCity = {},
            onRetry = {},
            onRefresh = {}
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
            locationState = LocationState.Idle,
            onAddLocation = {},
            onRetryLocation = {},
            onAddCityClick = {},
            onCityClick = {},
            onBack = {},
            onDeleteCity = {},
            onRetryCity = {},
            onRetry = {},
            onRefresh = {}
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
            locationState = LocationState.Idle,
            onAddLocation = {},
            onRetryLocation = {},
            onAddCityClick = {},
            onCityClick = {},
            onBack = {},
            onDeleteCity = {},
            onRetryCity = {},
            onRetry = {},
            onRefresh = {}
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

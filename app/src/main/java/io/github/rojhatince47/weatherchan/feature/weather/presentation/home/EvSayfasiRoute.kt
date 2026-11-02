package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import android.animation.ValueAnimator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.core.navigation.SELECTED_CITY_ID
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.theme.WeatherBackgroundPalette
import io.github.rojhatince47.weatherchan.core.ui.theme.resolveIsDark
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.presentation.detail.DetailUiState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.detail.ForecastContent

/**
 * Ev sayfasının yönlendirme katmanı: ViewModel durumunu ekrana bağlar ve
 * konum izni akışını yönetir.
 *
 * Snackbar bilinçli olarak YOKTUR: sayfa içi hata dalları `ForecastContent`
 * içindeki düğmelerle çözülür; silme snackbar'ı ince listede kalır.
 */
@Composable
fun EvSayfasiRoute(
    onAddCityClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenList: () -> Unit,
    modifier: Modifier = Modifier,
    // NavHost üstünde alınan ortak örnek: Ev ile Ayarlar aynı state machine'i
    // paylaşır (tek örnek); default yok — çağrı noktası açıkça verir.
    viewModel: EvSayfasiViewModel,
    // NavHost üstünde alınan ortak örnek; giriş başına ayrı örnek oluşmaz.
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    // Pager'ın kendi back stack girdisi: liste/ekleme dönüşünde seçilen
    // şehir kimliği bu kanaldan taşınır.
    savedStateHandle: SavedStateHandle? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

    // Liste ya da şehir ekleme ekranı kapandığında seçilen şehrin kimliği
    // buradan okunur; pager o sayfaya kaydırır ve anahtar temizlenir ki
    // aynı seçim ileri geri dönüşlerde yeniden tetiklenmesin.
    val selectCityId = savedStateHandle
        ?.getStateFlow<Long?>(SELECTED_CITY_ID, null)
        ?.collectAsStateWithLifecycle()
        ?.value
    LaunchedEffect(selectCityId) {
        if (selectCityId != null) {
            viewModel.selectCity(selectCityId)
            savedStateHandle?.set(SELECTED_CITY_ID, null)
        }
    }

    // "Konumumu ekle" ve "Tekrar dene" aynı kök eylemi paylaşır; izin
    // diyaloğu, otomatik ilk soruş ve karar ortak hook'tadır (Ayarlar ve
    // Şehirlerim de aynı hook'u kullanır).
    val onLocationAction = rememberLocationAction(
        permissionState = uiState.permissionState,
        onPermissionResult = viewModel::onPermissionResult,
        onReadLocation = viewModel::refreshLocation
    )

    // resolveIsDark composable'dır; lambda gövdesinden çağrılamaz (bir kez çöz).
    val isDark = themeMode.resolveIsDark()
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()

    // Arka plan paleti ile içerik rengi aynı kaynaktan gelir (DRY): veri
    // yoksa null → çağıran yerlerde neutral(isDark) karşılığı kullanılır.
    fun paletteFor(city: City): WeatherBackgroundPalette? {
        val detail = uiState.forecasts[city.id]
        val current = (detail?.content as? UiState.Success)?.data?.currentWeather
        return if (current != null) {
            WeatherBackgroundPalette.of(
                condition = current.condition,
                isDark = isDark,
                isDay = current.isDay
            )
        } else {
            null
        }
    }

    EvSayfasiScreen(
        cities = uiState.pages,
        currentPage = uiState.currentPage,
        contentFor = { city ->
            val detail = uiState.forecasts[city.id] ?: DetailUiState(content = UiState.Loading)
            ForecastContent(
                uiState = detail,
                onRetry = { viewModel.onRetry(city.id) },
                onRefresh = { viewModel.onRefresh(city.id) },
                isDark = isDark,
                contentColor = (paletteFor(city) ?: WeatherBackgroundPalette.neutral(isDark)).contentColor,
                animationsEnabled = animationsEnabled
            )
        },
        backgroundPaletteFor = ::paletteFor,
        themeMode = themeMode,
        onPageChanged = viewModel::onPageChanged,
        onAddCityClick = onAddCityClick,
        onSettingsClick = onSettingsClick,
        onSelectCity = { selectedCityId ->
            // null = "listeyi aç" sinyali; liste seçiminden pager'a dönüş
            // Task 7'de kablolandı.
            if (selectedCityId == null) onOpenList()
        },
        onRetry = viewModel::onRetry,
        onRefresh = viewModel::onRefresh,
        locationState = uiState.locationState,
        onAddLocation = onLocationAction,
        onRetryLocation = onLocationAction,
        modifier = modifier
    )
}

package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.core.navigation.SELECTED_CITY_ID
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.detail.DetailUiState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.detail.ForecastContent
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationPermissionState

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
    viewModel: EvSayfasiViewModel = hiltViewModel(),
    // NavHost üstünde alınan ortak örnek; giriş başına ayrı örnek oluşmaz.
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    // Pager'ın kendi back stack girdisi: liste/ekleme dönüşünde seçilen
    // şehir kimliği bu kanaldan taşınır.
    savedStateHandle: SavedStateHandle? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

    // Sistem izin diyaloğu. Cevap ViewModel'e bırakılır; kalıcı bayrak orada
    // yazılır ve bu launcher bir daha tetiklenmez.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // Kaba konum izni tek başına da yeterlidir; kullanıcı "yaklaşık"
        // seçtiyse akış yine çalışır.
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted = granted)
    }

    // Diyaloğu yalnız **ilk** açılışta göster. `askedKey` bu composable'ın
    // ömrü boyunca "gösterildi" işaretini tutar; kalıcı karar ViewModel'deki
    // bayraktadır. Etkinlik yeniden oluşsa bile diyalog ikinci kez açılmaz.
    var askedKey by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.permissionState) {
        if (!askedKey && uiState.permissionState == LocationPermissionState.NotAsked) {
            askedKey = true
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    // "Konumumu ekle" ve "Tekrar dene" aynı kök eylemi paylaşır: kullanıcının
    // konumunu istemek. Eylem, **o anki** gerçek izin durumundan seçilir —
    // izin yokken konum okumak tanımı gereği başarısız olur; yeniden okumak
    // değil izni geri kazanmak gerekir.
    val onLocationAction = {
        when (locationActionFor(context.hasLocationPermission(), context.canShowLocationRationale())) {
            LocationAction.ReadLocation -> viewModel.refreshLocation()
            LocationAction.RequestPermission -> permissionLauncher.launch(LOCATION_PERMISSIONS)
            LocationAction.OpenAppSettings -> context.openAppSettings()
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
                onRefresh = { viewModel.onRefresh(city.id) }
            )
        },
        themeMode = themeMode,
        onPageChanged = viewModel::onPageChanged,
        onAddCityClick = onAddCityClick,
        onSettingsClick = onSettingsClick,
        onSelectCity = { selectedCityId ->
            // null = "listeyi aç" sinyali; liste seçiminden pager'a dönüş
            // Task 7'de kablonir.
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

/** Konum izinleri: kesin + kaba. Kaba tek başına da yeterlidir. */
private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

/**
 * Kullanıcı konumunu istediğinde (ekle ya da tekrar dene) uygulamanın
 * hangi eylemi seçeceği.
 */
internal enum class LocationAction {
    /** İzin var ama konum okunamadı (GPS kapalı, sinyal yok): yeniden oku. */
    ReadLocation,

    /** İzin yok ama sistem diyaloğu hâlâ gösterilebilir: izni yeniden iste. */
    RequestPermission,

    /** Kalıcı redd: sistem diyaloğu gösterilmez, tek yol uygulama ayarları. */
    OpenAppSettings
}

/**
 * Konum eylemi **o anki** gerçek izin durumundan seçilir.
 *
 * Regresyon (cihazda gözlendi): izin reddedildikten sonra "Tekrar dene"
 * hiçbir şey yapmıyordu. Düğme yalnız konumu yeniden okuyordu;
 * `AndroidLocationProvider` izin yoksa null döndüğü için bu okuma tanımı
 * gereği başarısız olur, izin diyaloğu ise bir daha gösterilmezdi (ilk
 * soruşta kalıcı bayrak yazılır, durum `NotAsked`'a dönmez).
 *
 * Karar tablosu:
 * - izin VAR → oku (başarısızlık GPS/sinyal kaynaklıdır, yeniden denemek anlamlı)
 * - izin YOK + diyalog gösterilebilir → izni yeniden iste (bir kez reddedilmiş
 *   durum; Android ikinci reddedene kadar diyaloğu göstermeye devam eder)
 * - izin YOK + diyalog kapalı → ayarları aç (kalıcı redd; `requestPermissions`
 *   sessizce anında reddedilir, düğme yine hiçbir şey yapmaz)
 */
internal fun locationActionFor(hasPermission: Boolean, canShowRationale: Boolean): LocationAction = when {
    hasPermission -> LocationAction.ReadLocation
    canShowRationale -> LocationAction.RequestPermission
    else -> LocationAction.OpenAppSettings
}

/**
 * Anlık izin denetimi. Kalıcı olmayabilir — kullanıcı ayarlardan izni geri
 * alabilir; bu yüzden durum önbelleğe değil her seferinde sorgulanır.
 */
private fun Context.hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
    PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
    PackageManager.PERMISSION_GRANTED

/**
 * Sistem, kullanıcıdan bir kez daha izin istenebilir mi? Bir reddden sonra
 * `true` döner; kalıcı reddde ("bir daha sorma" ya da ikinci red)
 * `false` — bu durumda tek yol uygulama ayarlarıdır.
 */
private fun Context.canShowLocationRationale(): Boolean {
    val activity = this as? Activity ?: return false
    return activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
        activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
}

/** Kalıcı reddedilen izin için kullanıcıyı uygulamanın ayar sayfasına götürür. */
private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
    )
}

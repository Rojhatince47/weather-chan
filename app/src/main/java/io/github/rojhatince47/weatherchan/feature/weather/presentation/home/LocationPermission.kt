package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.rojhatince47.weatherchan.core.common.location.hasLocationPermission
import io.github.rojhatince47.weatherchan.core.common.location.isLocationEnabled
import io.github.rojhatince47.weatherchan.core.common.location.openAppSettings
import io.github.rojhatince47.weatherchan.core.common.location.openLocationSettings
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationPermissionState

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
    /** İzin var, konum servisi açık: konumu yeniden oku. */
    ReadLocation,

    /** İzin yok ama sistem diyaloğu hâlâ gösterilebilir: izni yeniden iste. */
    RequestPermission,

    /** Kalıcı redd: sistem diyaloğu gösterilmez, tek yol uygulama ayarları. */
    OpenAppSettings,

    /** İzin var ama cihaz konumu kapalı: konum ayarlarını aç. */
    OpenLocationSettings
}

/**
 * Konum eylemi **o anki** gerçek izin ve cihaz konumu durumundan seçilir.
 *
 * Regresyon (cihazda gözlendi): izin reddedildikten sonra ya da cihaz
 * konumu kapalıyken "Tekrar dene" sessizce başarısız oluyordu; kullanıcıya
 * hiçbir geri dönüş yoktu. Karar tablosu:
 *
 * - izin VAR + konum KAPALI → konum ayarlarını aç (okumak tanımı gereği başarısız)
 * - izin VAR + konum AÇIK → oku (başarısızlık sinyal kaynaklıdır, yeniden denemek anlamlı)
 * - izin YOK + diyalog gösterilebilir → izni yeniden iste
 * - izin YOK + diyalog kapalı → uygulama ayarlarını aç (kalıcı redd)
 */
internal fun locationActionFor(hasPermission: Boolean, canShowRationale: Boolean, servicesEnabled: Boolean): LocationAction = when {
    hasPermission && !servicesEnabled -> LocationAction.OpenLocationSettings
    hasPermission -> LocationAction.ReadLocation
    canShowRationale -> LocationAction.RequestPermission
    else -> LocationAction.OpenAppSettings
}

/**
 * İzin diyaloğu + otomatik ilk soruş + eylem kararı için ortak hook.
 *
 * Ev, Ayarlar ve Şehirlerim route'u bu hook ile aynı state machine'e
 * bağlanır: launcher, `NotAsked` otomatik ateşi ve karar tek yerdedir;
 * state makinesi kalır (tek `EvSayfasiViewModel` örneği). Hook'u çağıran
 * her route mount olduğunda ateşleyebilir — bayrak `Asked` olduktan sonra
 * tekrar tetiklenmez.
 *
 * Dönen lambda "Konumumu ekle" ve "Tekrar dene" için tek kök eylemdir.
 */
@Composable
internal fun rememberLocationAction(
    permissionState: LocationPermissionState,
    onPermissionResult: (granted: Boolean) -> Unit,
    onReadLocation: () -> Unit
): () -> Unit {
    val context = LocalContext.current

    // Sistem izin diyaloğu. Cevap ViewModel'e bırakılır; kalıcı bayrak orada
    // yazılır ve bu launcher bir daha tetiklenmez.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // Kaba konum izni tek başına da yeterlidir; kullanıcı "yaklaşık"
        // seçtiyse akış yine çalışır.
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onPermissionResult(granted)
    }

    // Diyaloğu yalnız **ilk** açılışta göster. `askedKey` bu composable'ın
    // ömrü boyunca "gösterildi" işaretini tutar; kalıcı karar ViewModel'deki
    // bayraktadır. Etkinlik yeniden oluşsa bile diyalog ikinci kez açılmaz.
    var askedKey by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(permissionState) {
        if (!askedKey && permissionState == LocationPermissionState.NotAsked) {
            askedKey = true
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    // Eylem, **o anki** gerçek izin ve konum servisi durumundan seçilir:
    // izin yokken konum okumak ya da konum kapalıyken yeniden denemek tanımı
    // gereği başarısız olur; kullanıcı ayarlara yönlendirilir.
    return {
        when (
            locationActionFor(
                hasPermission = context.hasLocationPermission(),
                canShowRationale = context.canShowLocationRationale(),
                servicesEnabled = context.isLocationEnabled()
            )
        ) {
            LocationAction.ReadLocation -> onReadLocation()
            LocationAction.RequestPermission -> permissionLauncher.launch(LOCATION_PERMISSIONS)
            LocationAction.OpenAppSettings -> context.openAppSettings()
            LocationAction.OpenLocationSettings -> context.openLocationSettings()
        }
    }
}

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

package io.github.rojhatince47.weatherchan.core.common.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Konum için anlık durum denetimleri ve ayar bağlantıları. Hem konum ekranı
 * hem de ekleme satırı aynı bakışı kullanır; kullanıcı neden konum
 * alınamadığını görebilir.
 */
fun Context.hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
    PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
    PackageManager.PERMISSION_GRANTED

/** Cihaz konumu (GPS/ağ) açık mı? İzin olsa bile kapalıysa konum okunamaz. */
fun Context.isLocationEnabled(): Boolean {
    val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return true
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        manager.isLocationEnabled
    } else {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

/**
 * Konum neden okunamadı? UI bunu kullanıcıya söyler; "Tekrar dene" düğmesi
 * sessizce hiçbir şey yapmasın.
 */
enum class LocationProblem {
    /** Bilinen bir neden yok; genel "tekrar dene" durumu. */
    NONE,

    /** İzin verilmemiş. */
    PERMISSION_DENIED,

    /** Cihaz konumu (GPS/ağ) kapalı. */
    SERVICES_DISABLED,

    /** İzin ve konum açık ama okuma başarısız (sinyal, geçici hata). */
    UNKNOWN
}

/** Anlık izin + cihaz konumu durumundan problemi üretir. */
fun locationProblemFor(hasPermission: Boolean, servicesEnabled: Boolean): LocationProblem = when {
    !hasPermission -> LocationProblem.PERMISSION_DENIED
    !servicesEnabled -> LocationProblem.SERVICES_DISABLED
    else -> LocationProblem.UNKNOWN
}

/** Kalıcı reddedilen izin için uygulamanın ayar sayfasını açar. */
fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
    )
}

/** Cihaz konumu kapalıysa konum ayarlarını açar. */
fun Context.openLocationSettings() {
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}

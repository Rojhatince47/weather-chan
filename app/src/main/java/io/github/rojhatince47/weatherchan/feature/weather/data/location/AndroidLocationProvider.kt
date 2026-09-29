package io.github.rojhatince47.weatherchan.feature.weather.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executor
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [LocationManager] tabanlı [LocationProvider] uygulaması.
 *
 * **Neden Play Services yok:** Google Play servisleri cihazda bulunmayabilir
 * ve ek bağımlılık getirir. `LocationManager` her Android cihazda vardır; bu
 * uygulama tek modülle ve sıfır yeni bağımlılıkla çalışır.
 *
 * İki farklı API yolu kullanılır:
 * - **API 30+** → [LocationManager.getCurrentLocation], tek seferlik konum.
 *   Sürekli dinleme kurmaz, pil dostudur.
 * - **API 26–29** → son bilinen konum, yoksa tek seferlik güncelleme isteği.
 */
@Singleton
class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationProvider {

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /**
     * Sistem tanımlı bekleme süresi. Kullanıcı "Konumumu ekle" dediğinde
     * sonsuza kadar beklemek kabul edilemez; 12 saniye sonunda akış
     * kullanıcıya döner.
     */
    private val timeoutMillis = 12_000L

    /**
     * Konum okunamasa bile akış **asla** hata fırlatmaz; `null` döner.
     *
     * `SecurityException`, `IllegalArgumentException` ve
     * `CancellationException` dahil her şey yutulur: eksik konum bir hata
     * değil, veri eksikliğidir ve çağıran taraf `null` gördüğünde boş durum
     * gösterebilir. Yalnız coroutine iptali dışarı sızmamalıdır; bu yüzden
     * `withTimeoutOrNull` dışarıda ve hata yakalama onun *içinde* tutulur.
     */
    override suspend fun currentCoordinates(): Coordinates? {
        val manager = locationManager ?: return null
        if (!hasLocationPermission()) return null

        val location = withTimeoutOrNull(timeoutMillis) {
            runCatching {
                val provider = manager.firstEnabledProvider() ?: return@runCatching null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    manager.currentLocation(provider)
                } else {
                    manager.legacyLocation()
                }
            }.getOrNull()
        }
        return location?.toCoordinates()
    }

    /**
     * Konum izni verilmemişse `null`.
     *
     * Sistem diyaloğu kapatılmış olabilir; her kontrol **anlık** yapılır, ki
     * kullanıcı ayarlardan izni geri açtığında bir sonraki denemede görülsün.
     */
    private fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    private fun LocationManager.firstEnabledProvider(): String? = PROVIDER_PREFERENCE
        .firstOrNull { provider -> isProviderEnabled(provider) }

    /**
     * API 30+ tek seferlik konum.
     *
     * Metot `void` döner ve sonucu bir [Consumer] ile geri verir; bu yüzden
     * callback içinde continuation resume edilir. `CancellationSignal` iptal
     * halinde bekleyen sistem isteğini de düşürür — aksi halde askıya alınmış
     * continuation bellek sızdırır.
     */
    private suspend fun LocationManager.currentLocation(provider: String): Location? = suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }

        val executor = Executor { command -> command.run() }
        val consumer = Consumer { location: Location? ->
            if (continuation.isActive) continuation.resume(location) {}
        }
        getCurrentLocation(provider, signal, executor, consumer)
    }

    /**
     * API 26–29: son bilinen konum, yoksa tek seferlik güncelleme isteği.
     */
    @Suppress("DEPRECATION")
    private suspend fun LocationManager.legacyLocation(): Location? {
        PROVIDER_PREFERENCE
            .mapNotNull { provider -> getLastKnownLocation(provider) }
            .firstOrNull()
            ?.let { return it }
        return requestSingleUpdate()
    }

    @Suppress("DEPRECATION")
    private suspend fun LocationManager.requestSingleUpdate(): Location? = suspendCancellableCoroutine { continuation ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                removeUpdates(this)
                if (continuation.isActive) continuation.resume(location) {}
            }

            override fun onProviderDisabled(provider: String) = Unit
            override fun onProviderEnabled(provider: String) = Unit

            @Deprecated("Android 10 ve altı için zorunlu geri çağrım")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        val provider = firstEnabledProvider()
        if (provider == null) {
            continuation.resume(null) {}
            return@suspendCancellableCoroutine
        }
        // İptal edilirse dinleme de kaldırılır.
        continuation.invokeOnCancellation { removeUpdates(listener) }
        requestSingleUpdate(provider, listener, null)
    }

    private fun Location.toCoordinates() = Coordinates(latitude = latitude, longitude = longitude)

    private companion object {
        /**
         * Sağlayıcı tercih sırası. Ağ sağlayıcısı hızlıdır ve kapalı mekânlarda
         * çalışır; GPS daha yavaştır ama açık alanda kesindir. Pasif sağlayıcı
         * başka uygulamaların bıraktığı konumu verir.
         */
        val PROVIDER_PREFERENCE = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
    }
}

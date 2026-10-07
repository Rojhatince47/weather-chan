package io.github.rojhatince47.weatherchan.feature.weather.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Diskteki son başarılı tahmin (şehir-başı).
 *
 * Tazelik burada yaşar ([isStale]); VM yalnız sorar, hesap yapmaz.
 */
data class CachedForecast(
    val fetchedAt: Instant,
    val forecast: Forecast
) {
    fun isStale(now: Instant): Boolean = isStale(fetchedAt, now)

    companion object {
        val TTL: Duration = Duration.ofMinutes(30)

        fun isStale(fetchedAt: Instant, now: Instant): Boolean {
            val age = Duration.between(fetchedAt, now)
            return age.isNegative || age >= TTL
        }

        /**
         * Konum kaydının sabit önbellek anahtarı. Konum kimliği her açılışta
         * döndüğü için (taze GPS düzeltmesi) kimlikle anahtarlama tohumu
         * kaybederdi; bu değer `City.id` erişilebilir aralığının dışındadır.
         */
        const val LOCATION_KEY: Long = Long.MIN_VALUE
    }
}

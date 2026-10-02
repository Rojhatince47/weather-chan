package io.github.rojhatince47.weatherchan.feature.weather.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Diskteki son başarılı turun tamamı.
 *
 * Tazelik burada yaşar ([isStale]); VM yalnız sorar, hesap yapmaz.
 */
data class CachedWeathers(
    val fetchedAt: Instant,
    val weathers: List<CityWeather>
) {
    fun isStale(now: Instant): Boolean = isStale(fetchedAt, now)

    companion object {
        val TTL: Duration = Duration.ofMinutes(30)

        fun isStale(fetchedAt: Instant, now: Instant): Boolean {
            val age = Duration.between(fetchedAt, now)
            return age.isNegative || age >= TTL
        }
    }
}

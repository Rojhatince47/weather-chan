package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import io.github.rojhatince47.weatherchan.feature.city.data.remote.overpass.OverpassApi
import io.github.rojhatince47.weatherchan.feature.city.data.remote.overpass.OverpassSuggestionMapper
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySeed
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.NearbyCitySuggestionProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import java.time.Clock
import java.time.Duration
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
internal class OverpassNearbyCitySuggestionProvider @Inject constructor(
    private val api: OverpassApi,
    private val cache: NearbyCachePreferences,
    private val languageProvider: () -> String,
    private val clock: Clock,
    private val mapper: OverpassSuggestionMapper
) : NearbyCitySuggestionProvider {

    override suspend fun nearby(origin: Coordinates): List<CitySeed>? {
        val cached = cache.read(origin)
        if (!cached.isNullOrEmpty() && cache.isFresh(origin, clock.instant())) {
            return cached
        }
        return refresh(origin)
    }

    override suspend fun refresh(origin: Coordinates): List<CitySeed>? {
        // Anti-çift-istek: `nearby()` soğuk/bayar önbellekte az önce canlı tur
        // attıysa (kayıt yaşı < ANTI_DUP) ViewModel'in arkasındaki tazeleme
        // aynı sorguyu hemen tekrarlamaz — açılışta iki identical Overpass
        // isteği rate-limit'e en çok maruz kalan andır.
        val cached = cache.read(origin)
        val fetchedAt = cache.fetchedAtOrNull(origin)
        if (!cached.isNullOrEmpty() && fetchedAt != null &&
            Duration.between(fetchedAt, clock.instant()) < ANTI_DUP
        ) {
            return cached
        }
        val query = "[out:json][timeout:10];node(around:$RADIUS_M,%.4f,%.4f)[place~\"city|town\"];out body 10;"
            .format(Locale.US, origin.latitude, origin.longitude)
        for (base in MIRRORS) {
            val response = runCatching {
                api.query("$base/api/interpreter", query)
            }.onFailure { if (it is CancellationException) throw it }
                .getOrNull() ?: continue
            val seeds = mapper.toSeeds(response.elements, preferEnglish = languageProvider() == "en")
            if (seeds.isNotEmpty()) {
                cache.write(origin, seeds)
                return seeds
            }
        }
        return cache.read(origin)
    }

    companion object {
        val MIRRORS = listOf("https://overpass-api.de", "https://overpass.kumi.systems")
        val ANTI_DUP: Duration = Duration.ofSeconds(60)
        const val RADIUS_M = 30_000
    }
}

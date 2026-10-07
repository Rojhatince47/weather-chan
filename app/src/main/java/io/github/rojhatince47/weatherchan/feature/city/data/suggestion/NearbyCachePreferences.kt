package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.rojhatince47.weatherchan.core.common.dispatcher.IoDispatcher
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySeed
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
internal class NearbyCachePreferences @Inject constructor(
    @NearbyDataStore private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val clock: Clock
) {
    private val json = Json { ignoreUnknownKeys = true }

    internal fun cellKey(origin: Coordinates): String =
        "%.1f,%.1f".format(Locale.US, floor(origin.latitude * 10.0) / 10.0, floor(origin.longitude * 10.0) / 10.0)

    internal suspend fun isFresh(origin: Coordinates, now: Instant): Boolean {
        val fetched = fetchedAtOrNull(origin) ?: return false
        return Duration.between(fetched, now) < TTL
    }

    /** Önbelleğin alınma zamanı; hücre eşleşmiyorsa ya da kayıt yoksa `null`. */
    internal suspend fun fetchedAtOrNull(origin: Coordinates): Instant? {
        val prefs = try {
            dataStore.data.first()
        } catch (_: Exception) {
            return null
        }
        val raw = prefs[KEY] ?: return null
        val cached = runCatching { json.decodeFromString<CachedNearbyDto>(raw) }.getOrNull() ?: return null
        if (cached.cell != cellKey(origin)) return null
        return Instant.ofEpochSecond(cached.fetchedAtEpochSec)
    }

    suspend fun read(origin: Coordinates): List<CitySeed>? = withContext(ioDispatcher) {
        val prefs = try {
            dataStore.data.first()
        } catch (_: IOException) {
            return@withContext null
        }
        val raw = prefs[KEY] ?: return@withContext null
        val cached = runCatching { json.decodeFromString<CachedNearbyDto>(raw) }.getOrNull() ?: return@withContext null
        if (cached.cell != cellKey(origin)) return@withContext null
        cached.seeds.map { dto ->
            CitySeed(
                name = dto.name,
                countryCode = "",
                countryName = "",
                coordinates = Coordinates(dto.lat, dto.lon)
            )
        }
    }

    suspend fun write(origin: Coordinates, seeds: List<CitySeed>) {
        withContext(ioDispatcher) {
            val dto = CachedNearbyDto(
                cell = cellKey(origin),
                fetchedAtEpochSec = clock.instant().epochSecond,
                seeds = seeds.map { SeedDto(name = it.name, lat = it.coordinates.latitude, lon = it.coordinates.longitude) }
            )
            try {
                dataStore.edit { prefs ->
                    prefs[KEY] = json.encodeToString(CachedNearbyDto.serializer(), dto)
                }
            } catch (_: IOException) {
                // ignore
            }
        }
    }

    @Serializable
    private data class CachedNearbyDto(
        @SerialName("cell") val cell: String,
        @SerialName("fetchedAtEpochSec") val fetchedAtEpochSec: Long,
        @SerialName("seeds") val seeds: List<SeedDto>
    )

    @Serializable
    private data class SeedDto(
        @SerialName("name") val name: String,
        @SerialName("lat") val lat: Double,
        @SerialName("lon") val lon: Double
    )

    companion object {
        val KEY = stringPreferencesKey("nearby_cache_v1")
        val TTL: Duration = Duration.ofHours(6)
    }
}

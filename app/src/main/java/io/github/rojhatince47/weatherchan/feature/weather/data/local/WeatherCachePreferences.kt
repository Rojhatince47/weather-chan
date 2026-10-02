package io.github.rojhatince47.weatherchan.feature.weather.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.rojhatince47.weatherchan.core.di.WeatherCacheDataStore
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Hava durumu önbelleğinin disk katmanı: tek blob + tek anahtar.
 *
 * Şehir listesinden **ayrı dosyadadır** (`@WeatherCacheDataStore`,
 * `hava_weather_cache`): aynı dosya paylaşılsaydı her önbellek yazma
 * `cities()` akışını yeniden yayımlatır ve tur-yaz-tur sonsuz döngüsü
 * oluşurdu. Bozuk içerik `null`'a düşer (önbellek-yok gibi davranılır).
 */
// NOT: `@Inject` YOKTUR — aynı tip için `WeatherDataModule`'de `@Provides`
// vardır; ikisi birden Dagger çift-bağlama hatası üretir.
@Singleton
class WeatherCachePreferences constructor(
    @WeatherCacheDataStore private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val cacheKey = stringPreferencesKey("cached_weathers")

    // `internal`: DTO'lar `internal` olduğu için public imzada dışa açılamazlar;
    // aynı modül içinden erişim serbesttir.
    internal fun cached(): Flow<CachedWeathersDto?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[cacheKey]?.let { decode(it) } }

    internal suspend fun write(cached: CachedWeathersDto) {
        dataStore.edit { preferences ->
            preferences[cacheKey] = json.encodeToString(CachedWeathersDto.serializer(), cached)
        }
    }

    private fun decode(raw: String): CachedWeathersDto? = runCatching { json.decodeFromString<CachedWeathersDto>(raw) }.getOrNull()
}

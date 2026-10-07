package io.github.rojhatince47.weatherchan.feature.weather.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.rojhatince47.weatherchan.core.di.WeatherCacheDataStore
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
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

    private val forecastsKey = stringPreferencesKey("cached_forecasts")
    private val legacyCacheKey = stringPreferencesKey("cached_weathers")
    private val legacyCacheRemovedKey = booleanPreferencesKey("cached_weathers_removed")
    private val locationKey = stringPreferencesKey("location_city")

    // `internal`: DTO'lar `internal` olduğu için public imzada dışa açılamazlar;
    // aynı modül içinden erişim serbesttir.

    /**
     * Şehir-başı tahmin önbelleği. Şehir listesinden **ayrı dosyadadır**
     * (`@WeatherCacheDataStore`, `hava_weather_cache`): aynı dosya
     * paylaşılsaydı her önbellek yazma `cities()` akışını yeniden yayımlatır
     * ve tur-yaz-tur sonsuz döngüsü oluşurdu.
     *
     * Okuma, ilk kullanımdan önce eski blob anahtarını bir kez temizler.
     * Bozuk içerik `null`'a düşer (önbellek-yok gibi davranılır); giriş
     * düzeyi bozukluk repository katmanında ele alınır.
     */
    internal fun cachedForecasts(): Flow<Map<Long, CachedForecastDto>?> = flow {
        removeLegacyCacheOnce()
        emitAll(
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { preferences -> preferences[forecastsKey]?.let { decodeForecasts(it) } }
        )
    }

    internal suspend fun writeForecast(key: Long, dto: CachedForecastDto) {
        // Birleştirme `edit` dönüşümü içindedir: pager penceresi ~3 eşzamanlı
        // çağrı üretir; dönüşüm dışı okuma kayıp güncelleme yapardı.
        dataStore.edit { preferences ->
            val current = preferences[forecastsKey]?.let { decodeForecasts(it) }.orEmpty().toMutableMap()
            current[key] = dto
            preferences[forecastsKey] = json.encodeToString(MapSerializer(Long.serializer(), CachedForecastDto.serializer()), current)
        }
    }

    private fun decodeForecasts(raw: String): Map<Long, CachedForecastDto>? = runCatching {
        json.decodeFromString(MapSerializer(Long.serializer(), CachedForecastDto.serializer()), raw)
    }.getOrNull()

    private suspend fun removeLegacyCacheOnce() {
        dataStore.edit { preferences ->
            if (preferences[legacyCacheRemovedKey] != true) {
                preferences.remove(legacyCacheKey)
                preferences[legacyCacheRemovedKey] = true
            }
        }
    }

    internal fun locationCity(): Flow<CityDto?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[locationKey]?.let { decodeLocation(it) } }

    internal suspend fun writeLocationCity(city: CityDto) {
        dataStore.edit { preferences ->
            preferences[locationKey] = json.encodeToString(CityDto.serializer(), city)
        }
    }

    private fun decodeLocation(raw: String): CityDto? = runCatching { json.decodeFromString<CityDto>(raw) }.getOrNull()
}

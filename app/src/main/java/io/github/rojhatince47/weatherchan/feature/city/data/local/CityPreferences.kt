package io.github.rojhatince47.weatherchan.feature.city.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.rojhatince47.weatherchan.core.di.CityDataStore
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * DataStore anahtarları ve şehir serileştirmesi.
 *
 * Anahtarlar:
 * - `cities_json`: şehir listesi, JSON dizisi, ekleme sırası korunur.
 * - `location_permission_asked`: konum izni bir kez soruldu mu.
 * - `cities_migrated_v2`: gömülü tohum listesi bir kez temizlendi mi.
 * - `stale_cache_removed`: hatalı paylaşılan önbellek anahtarı bir kez temizlendi mi.
 */
@Singleton
class CityPreferences @Inject constructor(
    @CityDataStore private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val citiesKey = stringPreferencesKey("cities_json")
    private val permissionAskedKey = booleanPreferencesKey("location_permission_asked")
    private val seedRemovedKey = booleanPreferencesKey("cities_migrated_v2")
    private val staleCacheKey = stringPreferencesKey("cached_weathers")
    private val staleCacheRemovedKey = booleanPreferencesKey("stale_cache_removed")

    /**
     * Gömülü 32 şehirlik tohum listesi **bir kez** temizlenir.
     *
     * Ürün kararı: açılışta liste yalnız mevcut konumu içermelidir. Daha önce
     * kurulmuş sürümlerde `cities_json` içinde o tohum ve kullanıcının eklediği
     * şehirler birlikte durur; bayrak yazılmadıkça liste bir kez boşaltılır.
     * Bayrak aynı `edit` içinde yazıldığı için göç ya tamamen olur ya hiç
     * olmaz — yarı kalmış bir durum oluşamaz.
     */
    private suspend fun removeLegacySeedOnce() {
        dataStore.edit { preferences ->
            if (preferences[seedRemovedKey] != true) {
                preferences[citiesKey] = "[]"
                preferences[seedRemovedKey] = true
            }
        }
    }

    /**
     * Hatalı kablolama döneminde (`@CityDataStore` paylaşımı) önbellek blob'u
     * bu dosyaya yazılmıştı. Tek seferlik temizlik; bayrak aynı `edit`
     * içinde yazılır.
     */
    private suspend fun removeStaleCacheKeyOnce() {
        dataStore.edit { preferences ->
            if (preferences[staleCacheRemovedKey] != true) {
                preferences.remove(staleCacheKey)
                preferences[staleCacheRemovedKey] = true
            }
        }
    }

    /**
     * Depodaki şehir listesi.
     *
     * Göç, akışa **geçilmeden önce** çalışır: dinleyen ilk değer zaten temizlenmiş
     * gerçek listeyi görür, göçün kendisi ek bir karede görünmez.
     */
    fun cities(): Flow<List<City>> = flow {
        removeLegacySeedOnce()
        removeStaleCacheKeyOnce()
        emitAll(
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { preferences -> preferences[citiesKey]?.let { decode(it) } ?: emptyList() }
        )
    }

    suspend fun write(cities: List<City>) {
        dataStore.edit { preferences ->
            preferences[citiesKey] = json.encodeToString(cities.map { it.toDto() })
        }
    }

    suspend fun isLocationPermissionAsked(): Boolean = dataStore.data.first()[permissionAskedKey] == true

    suspend fun setLocationPermissionAsked() {
        dataStore.edit { it[permissionAskedKey] = true }
    }

    /**
     * Bozuk kayıtta uygulama çökmez, **boş listeye** düşülür; akış yarılmaz.
     * Kurtarma yalnız *çözülemediğinde* devreye girer: geçerli `"[]"` da boş
     * kalır, çünkü kullanıcı listeyi bilerek boşaltmıştır.
     */
    private fun decode(raw: String): List<City> = runCatching { json.decodeFromString<List<CityDto>>(raw).map { it.toCity() } }
        .getOrDefault(emptyList())

    /**
     * Diskteki kayıt.
     *
     * `isLocation` varsayılan değerlidir: bu alan eklenmeden yazılmış eski
     * kayıtlar çözümlenmeye devam eder ve `false` konum olarak yorumlanır.
     */
    @Serializable
    private data class CityDto(
        val name: String,
        val region: String? = null,
        val country: String? = null,
        val latitude: Double,
        val longitude: Double,
        val isLocation: Boolean = false
    )

    private fun City.toDto() = CityDto(
        name = name,
        region = region,
        country = country,
        latitude = coordinates.latitude,
        longitude = coordinates.longitude,
        isLocation = isLocation
    )

    private fun CityDto.toCity() = City(
        name = name,
        region = region,
        country = country,
        coordinates = Coordinates(latitude = latitude, longitude = longitude),
        isLocation = isLocation
    )
}

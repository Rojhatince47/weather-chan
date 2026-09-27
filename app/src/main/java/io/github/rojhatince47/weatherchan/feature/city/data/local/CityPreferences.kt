package io.github.rojhatince47.weatherchan.feature.city.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * DataStore anahtarları ve şehir serileştirmesi.
 *
 * Anahtarlar:
 * - `cities_json`: şehir listesi, JSON dizisi, favoriler üstte sıralı yazılır.
 * - `location_permission_asked`: konum izni bir kez soruldu mu (Plan 3'te kullanılır).
 */
@Singleton
class CityPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val citiesKey = stringPreferencesKey("cities_json")
    private val permissionAskedKey = booleanPreferencesKey("location_permission_asked")

    /**
     * Anahtar hiç yazılmadıysa gömülü liste döner (ilk açılış). Boş liste (`"[]"`)
     * yazılmışsa boş döner: kullanıcı listeyi bilerek boşaltmıştır, seed geri gelmez.
     */
    fun cities(): Flow<List<City>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val raw = preferences[citiesKey]
            if (raw == null) {
                DefaultCitySeed.cities
            } else {
                decode(raw)
            }
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
     * Bozuk kayıtta uygulama çökmek yerine gömülü listeye düşülür; akış yarılmaz.
     * Başarılı şekilde boş çözülen liste (`"[]"`) boş kalır: kullanıcı listeyi
     * bilerek boşaltmıştır, seed geri gelmez. Kurtarma yalnız *çözülemediğinde*
     * devreye girer.
     */
    private fun decode(raw: String): List<City> = runCatching { json.decodeFromString<List<CityDto>>(raw).map { it.toCity() } }
        .getOrDefault(DefaultCitySeed.cities)

    @Serializable
    private data class CityDto(
        val name: String,
        val region: String? = null,
        val country: String? = null,
        val latitude: Double,
        val longitude: Double,
        val isFavorite: Boolean = false
    )

    private fun City.toDto() = CityDto(
        name = name,
        region = region,
        country = country,
        latitude = coordinates.latitude,
        longitude = coordinates.longitude,
        isFavorite = isFavorite
    )

    private fun CityDto.toCity() = City(
        name = name,
        region = region,
        country = country,
        coordinates = Coordinates(latitude = latitude, longitude = longitude),
        isFavorite = isFavorite
    )
}

package io.github.rojhatince47.weatherchan.feature.city.domain.repository

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import kotlinx.coroutines.flow.StateFlow

/**
 * Kullanıcının şehir listesinin tek gerçek kaynağı.
 *
 * Favoriler ayrı bir depo değildir; [City.isFavorite] bir alandır. Bu yüzden
 * "listede olmayan şehir favorisi" gibi tutarsız bir durum oluşamaz.
 */
interface CityRepository {
    /** Her zaman favoriler en üstte sıralı döner. */
    val cities: StateFlow<List<City>>

    /**
     * Zaten listede olan şehir eklenirse mevcut kayıt korunur (yıldızı kaybolmaz).
     *
     * Mutatörler `suspend` ve **yazılan listeyi** döner: çağıran, yazma bittikten
     * sonra elindeki liste neyse onunla devam eder. Yazma asenkrondur (DataStore),
     * bu yüzden dönen değer `cities.value` okunmadan önce kullanılmalıdır;
     * aksi halde çağıran eski listeyi görüp yanlış veri çeker.
     */
    suspend fun add(city: City): List<City>

    suspend fun remove(cityId: Long): List<City>

    suspend fun toggleFavorite(cityId: Long): List<City>
}

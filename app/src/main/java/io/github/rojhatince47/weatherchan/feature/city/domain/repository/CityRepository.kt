package io.github.rojhatince47.weatherchan.feature.city.domain.repository

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import kotlinx.coroutines.flow.StateFlow

/**
 * Kullanıcının şehir listesinin tek gerçek kaynağı.
 */
interface CityRepository {
    /** Kullanıcının eklediği sırayı korur; yeniden sıralama yapılmaz. */
    val cities: StateFlow<List<City>>

    /**
     * Zaten listede olan şehir eklenirse mükerrer kayıt oluşmaz.
     *
     * Mutatörler `suspend` ve **yazılan listeyi** döner: çağıran, yazma bittikten
     * sonra elindeki liste neyse onunla devam eder. Yazma asenkrondur (DataStore),
     * bu yüzden dönen değer `cities.value` okunmadan önce kullanılmalıdır;
     * aksi halde çağıran eski listeyi görüp yanlış veri çeker.
     */
    suspend fun add(city: City): List<City>

    suspend fun remove(cityId: Long): List<City>
}

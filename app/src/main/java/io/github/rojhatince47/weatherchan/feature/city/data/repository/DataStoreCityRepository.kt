package io.github.rojhatince47.weatherchan.feature.city.data.repository

import io.github.rojhatince47.weatherchan.core.common.dispatcher.IoDispatcher
import io.github.rojhatince47.weatherchan.feature.city.data.local.CityPreferences
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityProximity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Singleton
class DataStoreCityRepository @Inject constructor(
    private val preferences: CityPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CityRepository {

    /**
     * Depo akışı doğrudan yayılır; `stateIn(...)` ile bir başlangıç değeri
     * eklenmez. DataStore ilk okumada gerçeği önbelleğe alır, dolayısıyla hem
     * doğruluk hem ek maliyet açısından bu biçim doğrudur.
     */
    override val cities: Flow<List<City>> = preferences.cities()

    override suspend fun add(city: City): List<City> = mutate { current ->
        // Zaten aynı yerde bir kayıt varsa üzerine yazma: mükerrer kayıt
        // oluşmasın. Kimlik değil yakınlık: aynı yer GPS/geocoding/asset/
        // Overpass koordinatlarında farklı `id` taşır (`CityProximity` KDoc).
        if (current.any { CityProximity.isSamePlace(it, city) }) current else current + city
    }

    override suspend fun remove(cityId: Long): List<City> = mutate { current ->
        current.filterNot { it.id == cityId }
    }

    override suspend fun restore(city: City, index: Int): List<City> = mutate { current ->
        if (current.any { CityProximity.isSamePlace(it, city) }) current else current.insertAt(index.coerceIn(0, current.size), city)
    }

    private fun <T> List<T>.insertAt(position: Int, element: T): List<T> = toMutableList().apply { add(position, element) }

    override suspend fun addAsLocation(city: City): List<City> = mutate { current ->
        // Konum tekildir. Ad çözümlemesi her açılışta yeniden koşar ve ağın
        // durumuna göre aynı yere farklı kimlik döndürebilir: başarılı çözüm
        // geocoding koordinatını, başarısız çözüm GPS koordinatını verir.
        // Bu yüzden kimliğe değil konum bayrağına **ve yakınlığa** bakılır.
        // Geocoding başarısızsa gelen kayıt isimsizdir; düşürülen isimli
        // kartın adı korunur — isimli kart boş isimli konuma dönüşmez.
        val sibling = current.firstOrNull { !it.isLocation && CityProximity.isSamePlace(it, city) }
        val location = city.copy(
            isLocation = true,
            name = city.name.ifBlank { sibling?.name.orEmpty() },
            region = city.region ?: sibling?.region,
            country = city.country ?: sibling?.country
        )
        // Konum başa; geri kalanlar eski sıralarını korur.
        listOf(location) + current.filterNot { it.isLocation || CityProximity.isSamePlace(it, location) }
    }

    /**
     * Oku-değiştir-yaz ve **yazılan listeyi** döndürür. Dönüş değeri, akışın
     * yeni değeri yaymasından bağımsızdır: çağıran aynı karede doğru listeyle
     * devam edebilsin diye. Yazma tek yerde toplanır.
     */
    private suspend fun mutate(block: (List<City>) -> List<City>): List<City> {
        val current = preferences.cities().first()
        val updated = block(current)
        preferences.write(updated)
        return updated
    }
}

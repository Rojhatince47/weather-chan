package io.github.rojhatince47.weatherchan.feature.city.data.repository

import io.github.rojhatince47.weatherchan.core.common.dispatcher.IoDispatcher
import io.github.rojhatince47.weatherchan.feature.city.data.local.CityPreferences
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
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
        // Zaten varsa üzerine yazma: mükerrer kayıt oluşmasın.
        if (current.any { it.id == city.id }) current else current + city
    }

    override suspend fun remove(cityId: Long): List<City> = mutate { current ->
        current.filterNot { it.id == cityId }
    }

    override suspend fun addAsLocation(city: City): List<City> = mutate { current ->
        val location = city.copy(isLocation = true)
        val withoutSame = current.filterNot { it.id == location.id }
        // Konum başa; geri kalanlar eski sıralarını korur.
        listOf(location) + withoutSame
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

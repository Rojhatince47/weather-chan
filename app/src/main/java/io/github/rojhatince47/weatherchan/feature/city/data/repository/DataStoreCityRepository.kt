package io.github.rojhatince47.weatherchan.feature.city.data.repository

import io.github.rojhatince47.weatherchan.core.common.dispatcher.IoDispatcher
import io.github.rojhatince47.weatherchan.feature.city.data.local.CityPreferences
import io.github.rojhatince47.weatherchan.feature.city.data.local.DefaultCitySeed
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

@Singleton
class DataStoreCityRepository @Inject constructor(
    private val preferences: CityPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CityRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override val cities: StateFlow<List<City>> = preferences.cities()
        .stateIn(scope, SharingStarted.Eagerly, DefaultCitySeed.cities)

    override suspend fun add(city: City): List<City> = mutate { current ->
        // Zaten varsa üzerine yazma: mükerrer kayıt oluşmasın.
        if (current.any { it.id == city.id }) current else current + city
    }

    override suspend fun remove(cityId: Long): List<City> = mutate { current ->
        current.filterNot { it.id == cityId }
    }

    /**
     * Oku-değiştir-yaz ve **yazılan listeyi** döndürür. Dönüş değeri, akışın
     * (`cities`) yeni değeri yaymasından bağımsızdır: çağıran aynı karede doğru
     * listeyle devam edebilsin diye. Yazma tek yerde toplanır.
     */
    private suspend fun mutate(block: (List<City>) -> List<City>): List<City> {
        val current = preferences.cities().first()
        val updated = block(current)
        preferences.write(updated)
        return updated
    }
}

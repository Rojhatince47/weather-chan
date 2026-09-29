package io.github.rojhatince47.weatherchan.feature.city.testing

import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test amaçlı şehir deposu. Varsayılan listesi eski örnek verinin 5 şehridir;
 * mevcut testlerin liste boyutu varsayımları korunur.
 */
class FakeCityRepository(
    initial: List<City> = listOf(
        City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98)),
        City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85)),
        City("İzmir", "Ege", "Türkiye", Coordinates(38.42, 27.14)),
        City("Antalya", "Akdeniz", "Türkiye", Coordinates(36.90, 30.70)),
        City("Trabzon", "Karadeniz", "Türkiye", Coordinates(41.00, 39.72))
    )
) : CityRepository {
    private val state = MutableStateFlow(initial)
    override val cities: StateFlow<List<City>> = state.asStateFlow()

    override suspend fun add(city: City): List<City> {
        val updated = if (state.value.any { it.id == city.id }) state.value else state.value + city
        state.value = updated
        return updated
    }

    override suspend fun remove(cityId: Long): List<City> {
        val updated = state.value.filterNot { it.id == cityId }
        state.value = updated
        return updated
    }
}

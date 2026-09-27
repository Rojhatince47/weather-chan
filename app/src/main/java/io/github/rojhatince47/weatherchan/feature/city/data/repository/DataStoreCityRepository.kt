package io.github.rojhatince47.weatherchan.feature.city.data.repository

import io.github.rojhatince47.weatherchan.core.common.dispatcher.IoDispatcher
import io.github.rojhatince47.weatherchan.feature.city.data.local.CityPreferences
import io.github.rojhatince47.weatherchan.feature.city.data.local.DefaultCitySeed
import io.github.rojhatince47.weatherchan.feature.city.domain.model.CitySort
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Singleton
class DataStoreCityRepository @Inject constructor(
    private val preferences: CityPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CityRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override val cities: StateFlow<List<City>> = preferences.cities()
        .map { CitySort.favoritesFirst(it) }
        .stateIn(scope, SharingStarted.Eagerly, DefaultCitySeed.cities)

    override fun add(city: City) = mutate { current ->
        // Zaten varsa üzerine yazma: aksi halde yıldız kaybolur.
        if (current.any { it.id == city.id }) current else current + city
    }

    override fun remove(cityId: Long) = mutate { current ->
        current.filterNot { it.id == cityId }
    }

    override fun toggleFavorite(cityId: Long) = mutate { current ->
        current.map { if (it.id == cityId) it.copy(isFavorite = !it.isFavorite) else it }
    }

    private fun mutate(block: (List<City>) -> List<City>) {
        scope.launch {
            val current = preferences.cities().first()
            preferences.write(CitySort.favoritesFirst(block(current)))
        }
    }
}

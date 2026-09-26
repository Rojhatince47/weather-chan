package io.github.rojhatince47.weatherchan.feature.favorites.data.repository

import io.github.rojhatince47.weatherchan.feature.favorites.domain.repository.FavoriteRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class InMemoryFavoriteRepository @Inject constructor() : FavoriteRepository {

    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    override val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

    override fun toggleFavorite(cityId: Long) {
        _favoriteIds.update { ids ->
            if (cityId in ids) {
                ids - cityId
            } else {
                ids + cityId
            }
        }
    }
}

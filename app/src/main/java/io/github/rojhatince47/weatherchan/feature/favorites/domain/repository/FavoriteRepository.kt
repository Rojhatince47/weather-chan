package io.github.rojhatince47.weatherchan.feature.favorites.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface FavoriteRepository {
    val favoriteIds: StateFlow<Set<Long>>

    fun toggleFavorite(cityId: Long)
}

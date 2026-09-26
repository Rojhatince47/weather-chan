package io.github.rojhatince47.weatherchan.feature.favorites.data.di

import io.github.rojhatince47.weatherchan.feature.favorites.data.repository.InMemoryFavoriteRepository
import io.github.rojhatince47.weatherchan.feature.favorites.domain.repository.FavoriteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FavoritesDataModule {

    @Binds
    abstract fun bindFavoriteRepository(impl: InMemoryFavoriteRepository): FavoriteRepository
}

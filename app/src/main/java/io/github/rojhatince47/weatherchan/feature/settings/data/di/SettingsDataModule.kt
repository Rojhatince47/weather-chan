package io.github.rojhatince47.weatherchan.feature.settings.data.di

import io.github.rojhatince47.weatherchan.feature.settings.data.repository.InMemoryTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.data.repository.InMemoryThemeModeRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.ThemeModeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsDataModule {

    @Binds
    abstract fun bindTemperatureUnitRepository(impl: InMemoryTemperatureUnitRepository): TemperatureUnitRepository

    @Binds
    abstract fun bindThemeModeRepository(impl: InMemoryThemeModeRepository): ThemeModeRepository
}

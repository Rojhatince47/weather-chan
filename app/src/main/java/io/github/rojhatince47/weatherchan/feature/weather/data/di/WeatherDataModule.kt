package io.github.rojhatince47.weatherchan.feature.weather.data.di

import io.github.rojhatince47.weatherchan.feature.weather.data.repository.WeatherRepositoryImpl
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WeatherDataModule {

    @Binds
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository
}

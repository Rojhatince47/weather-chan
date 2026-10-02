package io.github.rojhatince47.weatherchan.feature.weather.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.rojhatince47.weatherchan.core.di.CityDataStore
import io.github.rojhatince47.weatherchan.feature.weather.data.local.WeatherCachePreferences
import io.github.rojhatince47.weatherchan.feature.weather.data.location.AndroidLocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.data.repository.WeatherRepositoryImpl
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WeatherDataModule {

    @Binds
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository

    @Binds
    abstract fun bindLocationProvider(impl: AndroidLocationProvider): LocationProvider

    companion object {
        @Provides
        @Singleton
        fun provideWeatherCachePreferences(@CityDataStore dataStore: DataStore<Preferences>): WeatherCachePreferences =
            WeatherCachePreferences(dataStore)
    }
}

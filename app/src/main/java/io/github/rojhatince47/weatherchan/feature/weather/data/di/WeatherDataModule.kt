package io.github.rojhatince47.weatherchan.feature.weather.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.github.rojhatince47.weatherchan.core.di.WeatherCacheDataStore
import io.github.rojhatince47.weatherchan.feature.weather.data.local.WeatherCachePreferences
import io.github.rojhatince47.weatherchan.feature.weather.data.location.AndroidLocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.data.repository.WeatherRepositoryImpl
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.weatherCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "hava_weather_cache")

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
        @WeatherCacheDataStore
        fun provideWeatherCacheStore(@ApplicationContext context: Context): DataStore<Preferences> = context.weatherCacheDataStore

        @Provides
        @Singleton
        fun provideWeatherCachePreferences(@WeatherCacheDataStore dataStore: DataStore<Preferences>): WeatherCachePreferences =
            WeatherCachePreferences(dataStore)
    }
}

package io.github.rojhatince47.weatherchan.feature.city.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.github.rojhatince47.weatherchan.core.di.CityDataStore
import io.github.rojhatince47.weatherchan.feature.city.data.permission.DataStoreCityPermissions
import io.github.rojhatince47.weatherchan.feature.city.data.repository.DataStoreCityRepository
import io.github.rojhatince47.weatherchan.feature.city.domain.permission.CityPermissions
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.cityDataStore: DataStore<Preferences> by preferencesDataStore(name = "hava_city")

@Module
@InstallIn(SingletonComponent::class)
abstract class CityDataModule {

    @Binds
    abstract fun bindCityRepository(impl: DataStoreCityRepository): CityRepository

    @Binds
    abstract fun bindCityPermissions(impl: DataStoreCityPermissions): CityPermissions

    companion object {
        @Provides
        @Singleton
        @CityDataStore
        fun provideCityDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.cityDataStore
    }
}

package io.github.rojhatince47.weatherchan.feature.settings.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.rojhatince47.weatherchan.core.di.SettingsDataStore
import io.github.rojhatince47.weatherchan.feature.notifications.data.repository.DataStoreNotificationSettingsRepository
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationSettingsRepository
import io.github.rojhatince47.weatherchan.feature.settings.data.repository.AppCompatAppLanguageApplier
import io.github.rojhatince47.weatherchan.feature.settings.data.repository.DataStoreSoundEffectsRepository
import io.github.rojhatince47.weatherchan.feature.settings.data.repository.DataStoreTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.data.repository.DataStoreThemeModeRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.AppLanguageApplier
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.SoundEffectsRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.ThemeModeRepository
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "hava_settings")

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsDataModule {

    @Binds
    abstract fun bindTemperatureUnitRepository(impl: DataStoreTemperatureUnitRepository): TemperatureUnitRepository

    @Binds
    abstract fun bindThemeModeRepository(impl: DataStoreThemeModeRepository): ThemeModeRepository

    @Binds
    abstract fun bindAppLanguageApplier(impl: AppCompatAppLanguageApplier): AppLanguageApplier

    @Binds
    abstract fun bindSoundEffectsRepository(impl: DataStoreSoundEffectsRepository): SoundEffectsRepository

    @Binds
    abstract fun bindNotificationSettingsRepository(impl: DataStoreNotificationSettingsRepository): NotificationSettingsRepository

    companion object {
        @Provides
        @Singleton
        @SettingsDataStore
        fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.settingsDataStore
    }
}

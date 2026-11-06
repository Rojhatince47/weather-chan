package io.github.rojhatince47.weatherchan.feature.widget.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.rojhatince47.weatherchan.feature.widget.data.WidgetSnapshotStore
import io.github.rojhatince47.weatherchan.feature.widget.domain.WidgetSnapshotRepository
import io.github.rojhatince47.weatherchan.feature.widget.worker.GlanceWidgetRefresher
import io.github.rojhatince47.weatherchan.feature.widget.worker.WidgetRefresher
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WidgetSnapshotDataStore

@Module
@InstallIn(SingletonComponent::class)
object WidgetModule {

    @Provides
    @Singleton
    @WidgetSnapshotDataStore
    fun provideWidgetDataStore(@ApplicationContext context: Context): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        // Bozuk dosya widget'ı kilitlemesin; boş anlık görüntüyle devam edilir.
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { context.preferencesDataStoreFile("hava_widget") }
    )

    @Provides
    @Singleton
    fun provideWidgetRefresher(impl: GlanceWidgetRefresher): WidgetRefresher = impl

    @Provides
    @Singleton
    fun provideSnapshotRepository(impl: WidgetSnapshotStore): WidgetSnapshotRepository = impl
}

/** Glance, Hilt enjeksiyonu yapamadığı için store'a bu kapıdan erişir. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun snapshotStore(): WidgetSnapshotStore
}

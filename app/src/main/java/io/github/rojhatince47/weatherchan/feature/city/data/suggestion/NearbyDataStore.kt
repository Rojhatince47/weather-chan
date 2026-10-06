package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Qualifier

val Context.nearbyDataStore: DataStore<Preferences> by preferencesDataStore(name = "hava_nearby_cache")

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NearbyDataStore

package io.github.rojhatince47.weatherchan.feature.widget.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.rojhatince47.weatherchan.feature.widget.di.WidgetSnapshotDataStore
import io.github.rojhatince47.weatherchan.feature.widget.domain.WidgetSnapshot
import io.github.rojhatince47.weatherchan.feature.widget.domain.WidgetSnapshotRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/** Widget'ın okuyacağı son anlık görüntü; ayrı dosyada tutulur. */
@Singleton
class WidgetSnapshotStore @Inject constructor(
    @WidgetSnapshotDataStore private val dataStore: DataStore<Preferences>
) : WidgetSnapshotRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val snapshotKey = stringPreferencesKey("widget_snapshot")

    override suspend fun read(): WidgetSnapshot? = dataStore.data
        .catch { emit(emptyPreferences()) }
        .first()[snapshotKey]
        ?.let { raw ->
            runCatching {
                json.decodeFromString<WidgetSnapshotDto>(raw).toDomain()
            }.getOrNull()
        }

    override suspend fun write(snapshot: WidgetSnapshot) {
        dataStore.edit { it[snapshotKey] = json.encodeToString(snapshot.toDto()) }
    }
}

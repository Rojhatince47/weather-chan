package io.github.rojhatince47.weatherchan.feature.notifications.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.rojhatince47.weatherchan.core.di.SettingsDataStore
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationHistory
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationHistoryStore
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

/**
 * Geçmiş üç anahtarda tutulur: gün, tür sayaçları ve son gönderim anları.
 * Bozuk kayıt akışı yarılmaz; o tür yok sayılır.
 */
@Singleton
class DataStoreNotificationHistoryStore @Inject constructor(
    @SettingsDataStore private val dataStore: DataStore<Preferences>
) : NotificationHistoryStore {

    private val dayKey = stringPreferencesKey("notif_history_day")
    private val countsKey = stringPreferencesKey("notif_history_counts")
    private val lastKey = stringPreferencesKey("notif_history_last")

    override suspend fun read(today: LocalDate): NotificationHistory {
        val preferences = dataStore.data
            .catch { emit(emptyPreferences()) }
            .first()
        val day = preferences[dayKey]
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return NotificationHistory(today)
        if (day != today) return NotificationHistory(today)

        return NotificationHistory(
            day = day,
            sentCounts = preferences[countsKey].toCounts(),
            lastSentAt = preferences[lastKey].toLastSentAt()
        )
    }

    override suspend fun write(history: NotificationHistory) {
        dataStore.edit { preferences ->
            preferences[dayKey] = history.day.toString()
            preferences[countsKey] = history.sentCounts.entries.joinToString(",") {
                "${it.key.name}=${it.value}"
            }
            preferences[lastKey] = history.lastSentAt.entries.joinToString(",") {
                "${it.key.name}=${it.value.toEpochMilli()}"
            }
        }
    }

    private fun String?.toCounts(): Map<NotificationType, Int> {
        if (this.isNullOrBlank()) return emptyMap()
        return split(",").mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size != 2) return@mapNotNull null
            val type = NotificationType.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            val count = parts[1].toIntOrNull() ?: return@mapNotNull null
            type to count
        }.toMap()
    }

    private fun String?.toLastSentAt(): Map<NotificationType, Instant> {
        if (this.isNullOrBlank()) return emptyMap()
        return split(",").mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size != 2) return@mapNotNull null
            val type = NotificationType.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            val instant = parts[1].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return@mapNotNull null
            type to instant
        }.toMap()
    }
}

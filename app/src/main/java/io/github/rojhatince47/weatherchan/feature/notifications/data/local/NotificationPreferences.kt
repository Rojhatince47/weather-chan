package io.github.rojhatince47.weatherchan.feature.notifications.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.rojhatince47.weatherchan.core.di.SettingsDataStore
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Bildirim ayarlarının disk anahtarları. Uygulama ayarlarıyla aynı
 * DataStore dosyasında tutulur; şehir verisinden ayrıdır.
 *
 * Anahtarlar:
 * - `notif_master`: ana anahtar, yoksa açık.
 * - `notif_severe|rain|summary|companion`: tür anahtarları, yoksa açık.
 * - `notif_summary_time`: `HH:mm`, yoksa 08:00.
 */
@Singleton
class NotificationPreferences @Inject constructor(
    @SettingsDataStore private val dataStore: DataStore<Preferences>
) {
    private val masterKey = booleanPreferencesKey("notif_master")
    private val summaryTimeKey = stringPreferencesKey("notif_summary_time")
    private val typeKeys = mapOf(
        NotificationType.SEVERE_WEATHER to booleanPreferencesKey("notif_severe"),
        NotificationType.RAIN_SOON to booleanPreferencesKey("notif_rain"),
        NotificationType.DAILY_SUMMARY to booleanPreferencesKey("notif_summary"),
        NotificationType.COMPANION to booleanPreferencesKey("notif_companion")
    )

    fun settings(): Flow<NotificationSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            NotificationSettings(
                masterEnabled = preferences[masterKey] ?: true,
                typeEnabled = NotificationType.entries.associateWith { type ->
                    preferences[typeKeys.getValue(type)] ?: true
                },
                summaryTime = preferences[summaryTimeKey].toLocalTime()
            )
        }

    suspend fun writeMasterEnabled(enabled: Boolean) {
        dataStore.edit { it[masterKey] = enabled }
    }

    suspend fun writeTypeEnabled(type: NotificationType, enabled: Boolean) {
        dataStore.edit { it[typeKeys.getValue(type)] = enabled }
    }

    suspend fun writeSummaryTime(time: LocalTime) {
        dataStore.edit { it[summaryTimeKey] = time.toString() }
    }

    // Bozuk kayıt akışı yarılmaz; varsayılan saate düşülür.
    private fun String?.toLocalTime(): LocalTime = this?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        ?: NotificationSettings.DEFAULT_SUMMARY_TIME
}

package io.github.rojhatince47.weatherchan.feature.settings.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.rojhatince47.weatherchan.core.di.SettingsDataStore
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Ayar anahtarları.
 *
 * Şehir verisinden **ayrı dosyada** tutulur: ayar yazımı şehir listesini
 * etkileyemez, şehir silme/yazma işlemi de ayarları düşüremez.
 *
 * Anahtarlar:
 * - `theme_mode`: ThemeMode adı, yoksa SYSTEM.
 * - `temperature_unit`: TemperatureUnit adı, yoksa CELSIUS.
 */
@Singleton
class SettingsPreferences @Inject constructor(
    @SettingsDataStore private val dataStore: DataStore<Preferences>
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val unitKey = stringPreferencesKey("temperature_unit")

    fun themeMode(): Flow<ThemeMode> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[themeModeKey].toThemeMode() }

    fun unit(): Flow<TemperatureUnit> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[unitKey].toUnit() }

    suspend fun writeThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[themeModeKey] = themeMode.name }
    }

    suspend fun writeUnit(unit: TemperatureUnit) {
        dataStore.edit { it[unitKey] = unit.name }
    }

    // Anahtar yoksa ya da kayıt tanınmıyorsa varsayılana düşülür; akış
    // yarılmaz, uygulama açılır.
    private fun String?.toThemeMode(): ThemeMode = ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM

    private fun String?.toUnit(): TemperatureUnit = TemperatureUnit.entries.firstOrNull { it.name == this } ?: TemperatureUnit.CELSIUS
}

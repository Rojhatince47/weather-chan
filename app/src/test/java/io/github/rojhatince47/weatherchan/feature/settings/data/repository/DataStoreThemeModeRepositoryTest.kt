package io.github.rojhatince47.weatherchan.feature.settings.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.feature.settings.data.local.SettingsPreferences
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreThemeModeRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var file: File

    @Before
    fun setUp() {
        file = File(temporaryFolder.root, "settings.preferences_pb")
    }

    private fun TestScope.createDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
    ) { file }

    private fun TestScope.createRepository(dataStore: DataStore<Preferences>): DataStoreThemeModeRepository = DataStoreThemeModeRepository(
        preferences = SettingsPreferences(dataStore),
        ioDispatcher = UnconfinedTestDispatcher(testScheduler)
    )

    @Test
    fun `ilk acilista sistem temasi gelir`() = runTest {
        val repository = createRepository(createDataStore())
        runCurrent()

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.value)
    }

    @Test
    fun `yazilan tema akista yayilir`() = runTest {
        val repository = createRepository(createDataStore())

        repository.themeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
            repository.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem())
        }
    }

    @Test
    fun `yeni depo ayni dosyadan kaydedilmis temayi okur`() = runTest {
        val dataStore = createDataStore()
        createRepository(dataStore).setThemeMode(ThemeMode.LIGHT)
        runCurrent()

        // Aynı dosya, yeni depo: uygulama yeniden başladığında okuyacak okuyucu.
        val restarted = createRepository(dataStore)
        runCurrent()

        assertEquals(ThemeMode.LIGHT, restarted.themeMode.value)
        // Değer yalnızca akışta değil, diskte de duruyor. Süreç yeniden
        // başlamasını DataStore'un ikinci aktif instance kısıtı yüzünden
        // burada canlandıramayız; o cihazda doğrulanıyor.
        assertTrue(file.readText().contains("LIGHT"))
    }
}

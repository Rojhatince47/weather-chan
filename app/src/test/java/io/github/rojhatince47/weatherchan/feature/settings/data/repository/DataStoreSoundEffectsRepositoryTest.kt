package io.github.rojhatince47.weatherchan.feature.settings.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.feature.settings.data.local.SettingsPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSoundEffectsRepositoryTest {

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

    private fun TestScope.createRepository(dataStore: DataStore<Preferences>): DataStoreSoundEffectsRepository =
        DataStoreSoundEffectsRepository(
            preferences = SettingsPreferences(dataStore),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

    @Test
    fun `ilk acilista ses efektleri acik`() = runTest {
        val repository = createRepository(createDataStore())
        runCurrent()

        assertTrue(repository.enabled.value)
    }

    @Test
    fun `kapatma akista yayilir`() = runTest {
        val repository = createRepository(createDataStore())

        repository.enabled.test {
            assertTrue(awaitItem())
            repository.setEnabled(false)
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `yeni depo ayni dosyadan kapali ayari okur`() = runTest {
        val dataStore = createDataStore()
        createRepository(dataStore).setEnabled(false)
        runCurrent()

        val restarted = createRepository(dataStore)
        runCurrent()

        assertFalse(restarted.enabled.value)
        assertTrue(file.exists())
    }
}

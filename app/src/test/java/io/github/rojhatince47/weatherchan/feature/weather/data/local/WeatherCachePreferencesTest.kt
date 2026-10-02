package io.github.rojhatince47.weatherchan.feature.weather.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WeatherCachePreferencesTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var file: File

    @Before
    fun setUp() {
        file = File(temporaryFolder.root, "weather_cache.preferences_pb")
    }

    private fun TestScope.createPreferences(dataStore: DataStore<Preferences>) = WeatherCachePreferences(dataStore)

    private fun TestScope.createDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
    ) { file }

    private fun sampleDto() = CachedWeathersDto(
        fetchedAtEpochMillis = 1_750_000_000_000L,
        weathers = emptyList()
    )

    @Test
    fun `yazilan okunur`() = runTest {
        val preferences = createPreferences(createDataStore())
        preferences.write(sampleDto())
        assertEquals(sampleDto(), preferences.cached().first())
    }

    @Test
    fun `bosken null doner`() = runTest {
        assertNull(createPreferences(createDataStore()).cached().first())
    }

    @Test
    fun `bozuk icerik null doner`() = runTest {
        val dataStore = createDataStore()
        dataStore.edit { it[stringPreferencesKey("cached_weathers")] = "{ bozuk" }
        assertNull(createPreferences(dataStore).cached().first())
    }
}

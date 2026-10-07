package io.github.rojhatince47.weatherchan.feature.weather.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
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

    private fun sampleForecastEntry() = CachedForecastDto(
        fetchedAtEpochMillis = 1_750_000_000_000L,
        forecast = ForecastDto(
            city = City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85)).toDto(),
            current = CurrentWeatherDto(14, "CLOUDY", 6, 45, true),
            hourly = listOf(HourlyForecastDto(10, 15, "CLEAR", 5)),
            daily = listOf(DailyForecastDto(0, 8, 16, "PARTLY_CLOUDY"))
        )
    )

    @Test
    fun `yazilan girisler map olarak okunur`() = runTest {
        val preferences = createPreferences(createDataStore())
        preferences.writeForecast(42L, sampleForecastEntry())
        assertEquals(mapOf(42L to sampleForecastEntry()), preferences.cachedForecasts().first())
    }

    @Test
    fun `eski girisler yeni yazimda korunur`() = runTest {
        val preferences = createPreferences(createDataStore())
        preferences.writeForecast(1L, sampleForecastEntry())
        preferences.writeForecast(2L, sampleForecastEntry())

        assertEquals(setOf(1L, 2L), preferences.cachedForecasts().first()!!.keys)
    }

    @Test
    fun `bosken null doner`() = runTest {
        assertNull(createPreferences(createDataStore()).cachedForecasts().first())
    }

    @Test
    fun `bozuk icerik null doner`() = runTest {
        val dataStore = createDataStore()
        dataStore.edit { it[stringPreferencesKey("cached_forecasts")] = "{ bozuk" }
        assertNull(createPreferences(dataStore).cachedForecasts().first())
    }

    @Test
    fun `eski cached_weathers anahtari bir kez temizlenir`() = runTest {
        val dataStore = createDataStore()
        dataStore.edit { it[stringPreferencesKey("cached_weathers")] = """{"x":1}""" }
        createPreferences(dataStore).cachedForecasts().first()

        assertNull(dataStore.data.first()[stringPreferencesKey("cached_weathers")])
    }

    @Test
    fun `konum adi yazilir okunur`() = runTest {
        val preferences = createPreferences(createDataStore())
        val city = City("Kucukpazar", "Istanbul", "Turkey", Coordinates(41.0, 28.983))
        preferences.writeLocationCity(city.toDto())

        assertEquals(city, preferences.locationCity().first()?.toCity())
    }

    @Test
    fun `bozuk konum kaydi null doner`() = runTest {
        val dataStore = createDataStore()
        dataStore.edit { it[stringPreferencesKey("location_city")] = "{ bozuk" }
        assertNull(createPreferences(dataStore).locationCity().first())
    }
}

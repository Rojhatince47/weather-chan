package io.github.rojhatince47.weatherchan.feature.city.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.rojhatince47.weatherchan.feature.city.data.local.CityPreferences
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreCityRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var file: File

    private val bursa = City(
        name = "Bursa",
        region = "Marmara",
        country = "Türkiye",
        coordinates = Coordinates(latitude = 40.19, longitude = 29.06)
    )
    private val ankara = City(
        name = "Ankara",
        region = "İç Anadolu",
        country = "Türkiye",
        coordinates = Coordinates(latitude = 39.92, longitude = 32.85)
    )

    @Before
    fun setUp() {
        file = File(temporaryFolder.root, "cities.preferences_pb")
    }

    private fun TestScope.createDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
    ) { file }

    private fun TestScope.createRepository(dataStore: DataStore<Preferences>): DataStoreCityRepository {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return DataStoreCityRepository(
            preferences = CityPreferences(dataStore),
            ioDispatcher = dispatcher
        )
    }

    @Test
    fun `ilk acilista gomulu liste yazilir`() = runTest {
        val repository = createRepository(createDataStore())

        val names = repository.cities.first { it.isNotEmpty() }.map { it.name }
        assertTrue(names.contains("İstanbul"))
        assertTrue(names.contains("Ankara"))
    }

    @Test
    fun `favoriler en ustte sirali`() = runTest {
        val dataStore = createDataStore()
        val repository = createRepository(dataStore)
        repository.cities.first { it.isNotEmpty() }

        repository.toggleFavorite(ankara.id)

        assertEquals("Ankara", repository.cities.first { it.first().name == "Ankara" }.first().name)
    }

    @Test
    fun `ayni sehir iki kez eklenmez ve yildizi korunur`() = runTest {
        val dataStore = createDataStore()
        val repository = createRepository(dataStore)
        repository.cities.first { it.isNotEmpty() }

        repository.add(bursa)
        repository.toggleFavorite(bursa.id)
        repository.add(bursa.copy(name = "Bursa (farklı yazım)"))

        val matching = repository.cities.first { list -> list.count { it.id == bursa.id } == 1 }
            .filter { it.id == bursa.id }
        assertEquals(1, matching.size)
        assertTrue("Yıldız korunmalıydı", matching.first().isFavorite)
    }

    @Test
    fun `cikarilan sehir listeden gider`() = runTest {
        val dataStore = createDataStore()
        val repository = createRepository(dataStore)
        repository.cities.first { it.isNotEmpty() }

        repository.remove(bursa.id)

        assertTrue(repository.cities.first { list -> list.none { it.id == bursa.id } }.none { it.id == bursa.id })
    }

    @Test
    fun `tum sehirler silinince seed geri gelmez`() = runTest {
        val dataStore = createDataStore()
        val repository = createRepository(dataStore)
        val initial = repository.cities.first { it.isNotEmpty() }

        initial.forEach { repository.remove(it.id) }

        // Kullanıcı listeyi bilerek boşalttı; gömülü liste geri gelmemeli.
        assertTrue(repository.cities.first { it.isEmpty() }.isEmpty())
    }

    @Test
    fun `yazilan veri yeni repository okumasiyla korunur`() = runTest {
        val dataStore = createDataStore()
        val first = createRepository(dataStore)
        first.cities.first { it.isNotEmpty() }
        first.toggleFavorite(ankara.id)

        val second = createRepository(dataStore)

        val ankaraInSecond = second.cities
            .first { list -> list.any { it.id == ankara.id && it.isFavorite } }
            .first { it.id == ankara.id }
        assertTrue(ankaraInSecond.isFavorite)
    }

    @Test
    fun `izin bayragi kalici olur`() = runTest {
        val dataStore = createDataStore()
        val preferences = CityPreferences(dataStore)

        assertFalse(preferences.isLocationPermissionAsked())

        preferences.setLocationPermissionAsked()

        assertTrue(preferences.isLocationPermissionAsked())
    }

    @Test
    fun `bogzuk kayit kurtarilir`() = runTest {
        // Bozuk JSON: decode hatası akışı kırmamalı, gömülü listeye düşülmeli
        val dataStore = createDataStore()
        dataStore.edit { it[stringPreferencesKey("cities_json")] = "{{{{bozuk" }

        val repository = createRepository(dataStore)

        assertTrue(repository.cities.first { it.isNotEmpty() }.isNotEmpty())
    }

    @Test
    fun `eski kayittaki favori alani siralamayi degistirmez`() = runTest {
        val dataStore = createDataStore()
        dataStore.edit {
            it[stringPreferencesKey("cities_json")] =
                """[{"name":"Istanbul","region":"Marmara","country":"Turkiye","latitude":41.01,"longitude":28.98,"isFavorite":false},""" +
                """{"name":"Ankara","region":"Ic Anadolu","country":"Turkiye","latitude":39.92,"longitude":32.85,"isFavorite":true},""" +
                """{"name":"Izmir","region":"Ege","country":"Turkiye","latitude":38.42,"longitude":27.14,"isFavorite":false}]"""
        }

        val repository = createRepository(dataStore)

        val names = repository.cities.first { list -> list.size == 3 }.map { it.name }

        assertEquals(listOf("Istanbul", "Ankara", "Izmir"), names)
    }
}

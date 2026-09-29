package io.github.rojhatince47.weatherchan.feature.city.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

    /**
     * Göçü yapılmış bir kurulumu taklit eder: bayrak yazılıdır, dolayısıyla
     * liste bir daha temizlenmez. Serileştirme uyumluluğu gibi yalnız *göçten
     * sonra* geçerli olan davranışları sınayan testler bunu çağırır.
     */
    private suspend fun DataStore<Preferences>.markMigrated() {
        edit { it[booleanPreferencesKey("cities_migrated_v2")] = true }
    }

    // --- varsayılan liste -----------------------------------------------------

    @Test
    fun `ilk acilista liste bos`() = runTest {
        val repository = createRepository(createDataStore())

        assertTrue(
            "Gömülü tohum kaldırıldı; taze kurulumda liste boş olmalı",
            repository.cities.first().isEmpty()
        )
    }

    @Test
    fun `tum sehirler silinince liste bos kalir`() = runTest {
        val repository = createRepository(createDataStore())

        repository.add(bursa)
        repository.add(ankara)
        assertEquals(2, repository.cities.first().size)

        repository.remove(bursa.id)
        repository.remove(ankara.id)

        assertTrue(
            "Kullanıcı listeyi bilerek boşalttı; tohum geri gelmemeli",
            repository.cities.first().isEmpty()
        )
    }

    // --- göç -------------------------------------------------------------------

    @Test
    fun `eski kurulumdaki kayitlar bir kez temizlenir`() = runTest {
        val dataStore = createDataStore()
        // Göçten önceki kurulum: bayrak yok, eski şehirler yazılı.
        dataStore.edit {
            it[stringPreferencesKey("cities_json")] = """
                [{"name":"Istanbul","region":"Marmara","country":"Turkiye","latitude":41.01,"longitude":28.98}]
            """.trimIndent()
        }

        val repository = createRepository(dataStore)

        assertTrue(
            "Eski kayıtlar tek seferlik göçte temizlenmeli",
            repository.cities.first().isEmpty()
        )
    }

    @Test
    fun `goc sonrasi eklenen sehirler kalici olur`() = runTest {
        val dataStore = createDataStore()
        createRepository(dataStore).cities.first() // göçü tetikler
        dataStore.markMigrated()

        val first = createRepository(dataStore)
        first.add(bursa)

        val second = createRepository(dataStore)

        assertEquals(
            "Göç tamamlandıktan sonra eklenen şehir silinmemeli",
            listOf("Bursa"),
            second.cities.first().map { it.name }
        )
    }

    // --- ekleme / çıkarma ------------------------------------------------------

    @Test
    fun `ayni sehir iki kez eklenmez`() = runTest {
        val repository = createRepository(createDataStore())

        repository.add(bursa)
        repository.add(bursa.copy(name = "Bursa (farklı yazım)"))

        val matching = repository.cities.first { list -> list.any { it.id == bursa.id } }
            .filter { it.id == bursa.id }
        assertEquals(1, matching.size)
    }

    @Test
    fun `cikarilan sehir listeden gider`() = runTest {
        val repository = createRepository(createDataStore())
        repository.add(bursa)

        repository.remove(bursa.id)

        assertTrue(repository.cities.first().none { it.id == bursa.id })
    }

    @Test
    fun `yazilan veri yeni repository okumasiyla korunur`() = runTest {
        val dataStore = createDataStore()
        val first = createRepository(dataStore)
        first.add(bursa)
        first.add(ankara)
        first.remove(ankara.id)

        val second = createRepository(dataStore)

        assertEquals(
            listOf("Bursa"),
            second.cities.first().map { it.name }
        )
    }

    // --- izin bayrağı ---------------------------------------------------------

    @Test
    fun `izin bayragi kalici olur`() = runTest {
        val dataStore = createDataStore()
        val preferences = CityPreferences(dataStore)

        assertFalse(preferences.isLocationPermissionAsked())

        preferences.setLocationPermissionAsked()

        assertTrue(preferences.isLocationPermissionAsked())
    }

    // --- bozuk kayıt -----------------------------------------------------------

    @Test
    fun `bogzuk kayit akisi kirmaz ve bos listeye duser`() = runTest {
        val dataStore = createDataStore()
        dataStore.markMigrated()
        dataStore.edit { it[stringPreferencesKey("cities_json")] = "{{{{bozuk" }

        val repository = createRepository(dataStore)

        val decoded = repository.cities.first()
        assertTrue(
            "Bozuk kayıt tohuma düşmemeli; akış boş listeyle devam etmeli. Gelen: $decoded",
            decoded.isEmpty()
        )
    }

    @Test
    fun `goc sonrasi eski favori alani siralamayi degistirmez`() = runTest {
        val dataStore = createDataStore()
        dataStore.markMigrated()
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

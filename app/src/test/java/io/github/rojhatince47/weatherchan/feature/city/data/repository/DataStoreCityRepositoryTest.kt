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
import kotlinx.coroutines.launch
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

    // --- konum yükseltme ------------------------------------------------------

    @Test
    fun `konum olarak eklenen sehir mükerrer olmaz`() = runTest {
        val repository = createRepository(createDataStore())

        repository.add(bursa)
        repository.addAsLocation(bursa)

        val stored = repository.cities.first()
        assertEquals(1, stored.size)
        assertTrue(stored.single().isLocation)
    }

    @Test
    fun `konum olarak eklenen sehir en basa gider`() = runTest {
        val repository = createRepository(createDataStore())

        repository.add(ankara)
        repository.add(bursa)
        repository.addAsLocation(bursa)

        assertEquals(
            "Konum listenin başında olmalı; mevcut sıra korunmalı",
            listOf("Bursa", "Ankara"),
            repository.cities.first().map { it.name }
        )
    }

    @Test
    fun `aramadan eklenen ayni sehir konum olarak yukseltilir yeni kayit olusmaz`() = runTest {
        val repository = createRepository(createDataStore())

        // Kullanıcı önce aramadan ekledi, sonra "Konumumu ekle" dedi.
        repository.add(bursa.copy(name = "Bursa Kalesi"))
        repository.addAsLocation(bursa.copy(name = "Bursa", isLocation = true))

        val stored = repository.cities.first()
        assertEquals(1, stored.size)
        assertTrue(stored.single().isLocation)
        assertEquals(
            "Yükseltilen kayıt güncel adı almalı",
            "Bursa",
            stored.single().name
        )
    }

    /**
     * Konum her açılışta yeniden çözülür. Aynı şehir bulunduğunda **yeniden
     * yazılmamalıdır**: yazmak depoyu yeniden yayımlatır, liste toplayıcısı
     * bunu görüp tüm şehirler için hava durumunu ikinci kez ister. Yani
     * kullanıcı şehir eklemeden her uygulama açılışında ağ isteği sayısı
     * ikiye katlanır ve hız sınırına çarpma riski artar.
     */
    @Test
    fun `ayni konum tekrar cozulurse veri yeniden yazilmaz`() = runTest {
        val repository = createRepository(createDataStore())
        repository.addAsLocation(bursa)
        var emissions = 0
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.cities.collect { emissions++ }
        }

        repository.addAsLocation(bursa)

        assertEquals(
            "Liste değişmediyse akış yeni bir değer yaymamalı",
            1,
            emissions
        )
        job.cancel()
    }

    @Test
    fun `konum isareti diskte kalici olur`() = runTest {
        val dataStore = createDataStore()

        createRepository(dataStore).addAsLocation(bursa)

        val read = createRepository(dataStore).cities.first()
        assertTrue(read.single().isLocation)
    }

    // --- konum işareti serileştirmesi ---

    @Test
    fun `konum isareti yazilip okununca korunur`() = runTest {
        val dataStore = createDataStore()
        val first = createRepository(dataStore)
        first.cities.first() // göçü tetikler

        val preferences = CityPreferences(dataStore)
        preferences.write(listOf(bursa.copy(isLocation = true), ankara))

        val read = CityPreferences(dataStore).cities().first()

        assertEquals(
            "Konum olarak kaydedilen şehrin işareti yeni okumada da durmalı",
            listOf(true, false),
            read.map { it.isLocation }
        )
    }

    @Test
    fun `isLocation alani olmayan eski kayit varsayilan olarak false okunur`() = runTest {
        val dataStore = createDataStore()
        dataStore.markMigrated()
        // Bu alan eklenmeden yazılmış kayıt: çözümlenmeli, patlamamalı.
        dataStore.edit {
            it[stringPreferencesKey("cities_json")] = """
                [{"name":"Istanbul","region":"Marmara","country":"Turkiye","latitude":41.01,"longitude":28.98}]
            """.trimIndent()
        }

        val read = CityPreferences(dataStore).cities().first()

        assertEquals(listOf("Istanbul"), read.map { it.name })
        assertFalse(
            "Eski kayıtlar konum olarak işaretlenmemeli",
            read.single().isLocation
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

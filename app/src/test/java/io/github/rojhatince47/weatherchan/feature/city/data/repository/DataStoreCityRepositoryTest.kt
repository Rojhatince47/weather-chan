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
    private val izmir = City(
        name = "İzmir",
        region = "Ege",
        country = "Türkiye",
        coordinates = Coordinates(latitude = 38.42, longitude = 27.14)
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
    fun `hatali paylasilan onbellek anahtari bir kez temizlenir`() = runTest {
        val dataStore = createDataStore()
        // Hatalı kablolama döneminde `cached_weathers` şehir dosyasına
        // yazılmıştı; şehir listesi korunarak yalnız o anahtar düşer.
        dataStore.edit {
            it[stringPreferencesKey("cities_json")] = """
                [{"name":"Istanbul","region":"Marmara","country":"Turkiye","latitude":41.01,"longitude":28.98}]
            """.trimIndent()
            it[stringPreferencesKey("cached_weathers")] = """{"fetchedAtEpochMillis":1,"weathers":[]}"""
        }
        // Eski-tohum göçü listeyi silmesin diye bayrak kurulur.
        dataStore.markMigrated()

        val repository = createRepository(dataStore)
        repository.cities.first()

        val keys = dataStore.data.first().asMap().keys.map { key -> key.name }
        assertTrue(
            "Ölü önbellek anahtarı temizlenmeli",
            keys.none { it == "cached_weathers" }
        )
        assertTrue(
            "Şehir listesi göçten etkilenmemeli",
            repository.cities.first().any { it.name == "Istanbul" }
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

    /**
     * Geri al, şehri listenin **sonuna** değil eski yerine koyar. Aksi hâlde
     * kullanıcı kartı geri geldiğinde listenin tamamı yer değiştirir ve hangi
     * kartın neden yer değiştirdiği anlaşılmaz.
     */
    @Test
    fun `geri alinan sehir eski sirasina doner`() = runTest {
        val repository = createRepository(createDataStore())
        repository.add(bursa)
        repository.add(ankara)
        repository.add(izmir)

        repository.remove(ankara.id)
        repository.restore(ankara, index = 1)

        assertEquals(
            listOf(bursa.id, ankara.id, izmir.id),
            repository.cities.first().map { it.id }
        )
    }

    /** Sıra dışı indis güvenli: liste kısalmışsa sona eklenir, çökmez. */
    @Test
    fun `geri alma indisi gecersizse sehir sona eklenir`() = runTest {
        val repository = createRepository(createDataStore())
        repository.add(bursa)
        repository.remove(bursa.id)

        repository.restore(ankara, index = 99)

        assertEquals(listOf(ankara.id), repository.cities.first().map { it.id })
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

    /**
     * Regresyon (cihazda gözlendi): ad çözümlemesi her açılışta yeniden koşar
     * ve ağın durumuna göre aynı yere **farklı kimlik** döndürür — başarılı
     * çözüm wttr.in'in geocoding koordinatını, başarısız çözüm GPS
     * koordinatını verir (örn. 41.0/28.983 ile 41.009998/28.98). Eski konum
     * kaydı yalnız aynı kimlikte çıkarılsaydı kimlik farkı aynı yeri iki
     * konum kartı yapardı.
     *
     * Konum tekildir: yeni konum, eski kaydın kimliği ne olursa olsun yerini
     * alır. Bu sınama "ağ düzeldi, çözümleme başarılı oldu" yoludur.
     */
    @Test
    fun `cozumleme gelisince eski konum kaydi kimlik farki olsa bile kaldirilir`() = runTest {
        val repository = createRepository(createDataStore())
        val fallback = City("Konum", null, null, Coordinates(41.009998333333336, 28.98))
        val resolved = City("Kucukpazar", "Istanbul", "Turkey", Coordinates(41.0, 28.983))

        repository.addAsLocation(fallback)
        repository.addAsLocation(resolved)

        val stored = repository.cities.first()
        assertEquals("Aynı yer iki kart olmamalı", 1, stored.size)
        assertEquals("Yeni çözümlemenin adı kalmalı", "Kucukpazar", stored.single().name)
        assertTrue(stored.single().isLocation)
    }

    /**
     * Aynı tekillik diğer yönde de geçerli: ağ bozulunca çözümleme
     * başarısız olur ve yedek "Konum" kaydı (GPS koordinatlı) gelir. Bu,
     * kullanıcının cihazda gözlediği ikilenmeyi üreten asıl yoldu.
     */
    @Test
    fun `cozumleme bozulunca eski konum kaydi kimlik farki olsa bile kaldirilir`() = runTest {
        val repository = createRepository(createDataStore())
        val resolved = City("Kucukpazar", "Istanbul", "Turkey", Coordinates(41.0, 28.983))
        val fallback = City("Konum", null, null, Coordinates(41.009998333333336, 28.98))

        repository.addAsLocation(resolved)
        repository.addAsLocation(fallback)

        val stored = repository.cities.first()
        assertEquals("Aynı yer iki kart olmamalı", 1, stored.size)
        assertEquals("Yedek ad kalmalı", "Konum", stored.single().name)
        assertTrue(stored.single().isLocation)
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

    // --- yakınlık mükerrer kontrolü ---------------------------------------------

    @Test
    fun `kaynaklar arasi ayni yer mukerrer olusmaz`() = runTest {
        // Gerçek cihaz çifti: GPS 7 basamak ↔ geocoding 5 basamak.
        val repository = createRepository(createDataStore())
        repository.add(City("Mardin", "Mardin", "Turkey", Coordinates(37.3652283, 40.2676315)))
        repository.add(City("Derik", "Mardin", "Türkiye Cumhuriyeti", Coordinates(37.36336, 40.26473)))

        assertEquals(listOf("Mardin"), repository.cities.first().map { it.name })
    }

    @Test
    fun `uzak sehirler ayri kalir`() = runTest {
        val repository = createRepository(createDataStore())
        repository.add(bursa)
        repository.add(ankara)

        assertEquals(2, repository.cities.first().size)
    }

    @Test
    fun `konum ayni yerdeki isimli karti bolmez ve ismi korur`() = runTest {
        // Geocoding başarısız düşüşü: isimsiz konum kaydı, aynı-yer isimli
        // kartı ezmemeli — isim korunur (spec K2.2).
        val repository = createRepository(createDataStore())
        repository.add(City("Derik", "Mardin", "Türkiye", Coordinates(37.36336, 40.26473)))

        repository.addAsLocation(City("", null, null, Coordinates(37.3652283, 40.2676315)))

        val stored = repository.cities.first()
        assertEquals(1, stored.size)
        assertTrue(stored.single().isLocation)
        assertEquals("Derik", stored.single().name)
    }

    @Test
    fun `geri alma ayni yer karti dururken sessizce atlanir`() = runTest {
        val repository = createRepository(createDataStore())
        val derik = City("Derik", "Mardin", "Türkiye", Coordinates(37.36336, 40.26473))
        repository.add(derik)
        repository.remove(derik.id)
        // Aynı yere başka kaynaklı kayıt eklendi:
        repository.add(City("Mardin", "Mardin", "Turkey", Coordinates(37.3652283, 40.2676315)))

        val restored = repository.restore(derik, 0)

        assertEquals(listOf("Mardin"), restored.map { it.name })
    }
}

package io.github.rojhatince47.weatherchan.feature.weather.domain.usecase

import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider as Provider
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeWeatherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetLocationCityUseCaseTest {

    private val coordinates = Coordinates(latitude = 41.01, longitude = 28.98)

    @Test
    fun `konum alinamazsa sonuc null olur akis durmaz`() = runTest {
        val weather = FakeWeatherRepository()
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(coordinates = null),
            weatherRepository = weather
        )

        assertNull(useCase())

        // Konum okunmadıysa ağa hiç gidilmemeli.
        assertTrue(weather.searchRequests.isEmpty())
    }

    @Test
    fun `ad cozumlenir ve konum isareti tasinir`() = runTest {
        val weather = FakeWeatherRepository().apply {
            searchResult = AppResult.Success(
                // Çözülen kaydın koordinatı GPS'ten farklı: kullanılmamalı.
                listOf(City("Kuzguncuk", "Istanbul", "Türkiye", Coordinates(41.05, 29.05)))
            )
        }
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(coordinates),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        assertTrue("Sonuç konum olarak işaretlenmeli", city.isLocation)
        // Doğruluk > hassasiyet: bölge adı yerleşim adına tercih edilir.
        assertEquals("Istanbul", city.name)
        // Veri daima gerçek GPS koordinatından okunur; yerleşim merkezi değil.
        assertEquals(coordinates, city.coordinates)
        // Coğrafi çözümleme yapmayan ham sorgu gönderilmeli. Ondalık ayırıcı
        // nokta olmalı (Locale.US) ve hassasiyet 4 basamak.
        assertEquals(listOf("41.0100,28.9800"), weather.searchRequests)
    }

    @Test
    fun `ad cozumlenemezse yedek adla akis surer`() = runTest {
        val weather = FakeWeatherRepository().apply {
            searchResult = AppResult.Failure(AppError.Network)
        }
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(coordinates),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        // Akış durmaz; konum yine de eklenebilir olmalı. Ad çözülemediyse
        // boş kalır — presentation dile göre çözer (spec §7).
        assertTrue(city.isLocation)
        assertEquals("", city.name)
    }

    @Test
    fun `ad cozumlenir ama sonuc bos ise yedek ad kullanilir`() = runTest {
        val weather = FakeWeatherRepository().apply {
            searchResult = AppResult.Success(emptyList())
        }
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(coordinates),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        assertEquals("", city.name)
    }

    @Test
    fun `yakin konumda aga cikilmaz`() = runTest {
        val near = Coordinates(latitude = 41.0181, longitude = 28.98)
        val cached = City("Kucukpazar", "Istanbul", "Turkey", Coordinates(41.01, 28.98), isLocation = true)
        val weather = FakeWeatherRepository().apply { cachedLocationCity = cached }
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(near),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        assertEquals("Kucukpazar", city.name)
        assertTrue(city.isLocation)
        // Taze koordinat taşınır (kart kimliği güncel kalır).
        assertEquals(near, city.coordinates)
        // Ağ SIFIR, yazma SIFIR.
        assertTrue(weather.searchRequests.isEmpty())
        assertTrue(weather.savedLocationCities.isEmpty())
    }

    @Test
    fun `uzak konumda cozulur ve yazilir`() = runTest {
        val far = Coordinates(latitude = 41.0325, longitude = 28.98)
        val cached = City("Kucukpazar", "Istanbul", "Turkey", Coordinates(41.01, 28.98), isLocation = true)
        val resolved = City("Uskudar", "Istanbul", "Turkey", far)
        val weather = FakeWeatherRepository().apply {
            cachedLocationCity = cached
            searchResult = AppResult.Success(listOf(resolved))
        }
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(far),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        // Bölge adı gerçek addır; önbelleğe da o yazılır.
        assertEquals("Istanbul", city.name)
        assertTrue(city.isLocation)
        assertEquals(1, weather.searchRequests.size)
        assertEquals(listOf(city), weather.savedLocationCities)
        assertEquals("Istanbul", weather.savedLocationCities.single().name)
    }

    @Test
    fun `yedek ad yazilmaz`() = runTest {
        val cached = City("Kucukpazar", "Istanbul", "Turkey", Coordinates(41.01, 28.98), isLocation = true)
        val weather = FakeWeatherRepository().apply {
            cachedLocationCity = cached
            searchResult = AppResult.Failure(AppError.Network)
        }
        val useCase = GetLocationCityUseCase(
            // Kayıttan uzak koordinat (~2.5 km) → ıska → ağ yolu → hata → boş ad.
            locationProvider = FakeLocationProvider(Coordinates(41.0325, 28.98)),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        assertEquals("", city.name)
        assertTrue(weather.savedLocationCities.isEmpty())
    }

    @Test
    fun `cihazda olculen segirtme vurur`() = runTest {
        // Regresyon: emülatörde aynı noktada ölçülen iki GPS okuması
        // (41.0,28.983) → (41.01,28.98) arası 1.14 km'dir. 1.0 km eşikle
        // ıskalayıp her açılışta 10 sn'lik zinciri tetikliyordu.
        val cached = City("Kucukpazar", "Istanbul", "Turkey", Coordinates(41.0, 28.983), isLocation = true)
        val fresh = Coordinates(latitude = 41.01, longitude = 28.98)
        val weather = FakeWeatherRepository().apply { cachedLocationCity = cached }
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(fresh),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        assertEquals("Kucukpazar", city.name)
        assertEquals(fresh, city.coordinates)
        assertTrue(weather.searchRequests.isEmpty())
        assertTrue(weather.savedLocationCities.isEmpty())
    }

    @Test
    fun `sinir-deger vurur`() = runTest {
        // Sınır-dahil (`<=`) davranışın deterministik kilidi: 0.95 km güvenli
        // payla içeride (kayan-nokta marjı nm mertebesi). Tam 1.0000 km
        // test edilmez — marjı yoktur.
        val almost = Coordinates(latitude = 41.0185, longitude = 28.98) // ~0.95 km
        val cached = City("Kucukpazar", "Istanbul", "Turkey", Coordinates(41.01, 28.98), isLocation = true)
        val weather = FakeWeatherRepository().apply { cachedLocationCity = cached }
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(almost),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        assertEquals("Kucukpazar", city.name)
        assertTrue(weather.searchRequests.isEmpty())
    }

    @Test
    fun `konum yoksa dolu onbellege bakilmaz`() = runTest {
        val cached = City("Kucukpazar", "Istanbul", "Turkey", Coordinates(41.01, 28.98), isLocation = true)
        val weather = FakeWeatherRepository().apply { cachedLocationCity = cached }
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(null),
            weatherRepository = weather
        )

        assertNull(useCase())
        assertTrue(weather.searchRequests.isEmpty())
    }

    @Test
    fun `kayit yoksa aga gidilir ve yazilir`() = runTest {
        // Bozuk kayıt prefs katmanında `null`'a düşer; use case'e `null` gelir —
        // bu test o yolu kilitler (önbellek-yok = ilk kurulum davranışı).
        val weather = FakeWeatherRepository().apply {
            searchResult = AppResult.Success(
                listOf(City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98)))
            )
        }
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(coordinates),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        assertEquals("Marmara", city.name)
        assertEquals(1, weather.savedLocationCities.size)
    }

    @Test
    fun `bolge yoksa yerlesim adi kullanilir`() = runTest {
        val weather = FakeWeatherRepository().apply {
            searchResult = AppResult.Success(
                listOf(City("Kuzguncuk", null, "Türkiye", Coordinates(41.03, 29.03)))
            )
        }
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(coordinates),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        assertEquals("Kuzguncuk", city.name)
        assertEquals(coordinates, city.coordinates)
    }

    @Test
    fun `koordinat dizisi cozumlenen ad bos sayilir`() = runTest {
        val weather = FakeWeatherRepository().apply {
            // wttr.in boş alanlarda fallback olarak sorgunun kendisini
            // döndürebilir; bu "ad" sayılmaz (spec §7).
            searchResult = AppResult.Success(
                listOf(City("41.0100,28.9800", null, null, Coordinates(41.01, 28.98)))
            )
        }
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(coordinates),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        assertEquals("", city.name)
        assertTrue(weather.savedLocationCities.isEmpty())
    }
}

/** Testte kullanılan sabit konum. */
private class FakeLocationProvider(private val coordinates: Coordinates?) : Provider {
    var callCount: Int = 0
        private set

    override suspend fun currentCoordinates(): Coordinates? {
        callCount++
        return coordinates
    }
}

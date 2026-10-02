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
                listOf(City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98)))
            )
        }
        val useCase = GetLocationCityUseCase(
            locationProvider = FakeLocationProvider(coordinates),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        assertTrue("Sonuç konum olarak işaretlenmeli", city.isLocation)
        assertEquals("İstanbul", city.name)
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

        // Akış durmaz; konum yine de eklenebilir olmalı.
        assertTrue(city.isLocation)
        assertEquals("Konum", city.name)
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

        assertEquals("Konum", city.name)
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
        val far = Coordinates(latitude = 41.0199, longitude = 28.98)
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

        assertEquals("Uskudar", city.name)
        assertTrue(city.isLocation)
        assertEquals(1, weather.searchRequests.size)
        assertEquals(listOf(city), weather.savedLocationCities)
        assertEquals("Uskudar", weather.savedLocationCities.single().name)
    }

    @Test
    fun `yedek ad yazilmaz`() = runTest {
        val cached = City("Kucukpazar", "Istanbul", "Turkey", Coordinates(41.01, 28.98), isLocation = true)
        val weather = FakeWeatherRepository().apply {
            cachedLocationCity = cached
            searchResult = AppResult.Failure(AppError.Network)
        }
        val useCase = GetLocationCityUseCase(
            // Kayıttan uzak koordinat → ıska → ağ yolu → hata → "Konum" yedeği.
            locationProvider = FakeLocationProvider(Coordinates(41.0199, 28.98)),
            weatherRepository = weather
        )

        val city = requireNotNull(useCase())

        assertEquals("Konum", city.name)
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

        assertEquals("İstanbul", city.name)
        assertEquals(1, weather.savedLocationCities.size)
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

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

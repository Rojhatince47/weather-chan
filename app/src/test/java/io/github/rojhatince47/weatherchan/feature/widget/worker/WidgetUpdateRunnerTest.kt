package io.github.rojhatince47.weatherchan.feature.widget.worker

import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.city.testing.FakeCityRepository
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.WeatherAdviceEngine
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import io.github.rojhatince47.weatherchan.feature.widget.domain.WidgetSnapshot
import io.github.rojhatince47.weatherchan.feature.widget.domain.WidgetSnapshotRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetUpdateRunnerTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val now = Instant.parse("2026-11-06T12:00:00Z")
    private val city = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
    private val locationCity = City(
        "Ankara",
        "İç Anadolu",
        "Türkiye",
        Coordinates(39.92, 32.85),
        isLocation = true
    )

    private val weather = mockk<WeatherRepository>(relaxed = true)
    private val store = FakeSnapshotRepository()
    private val refresher = RecordingRefresher()

    private fun runner(cities: List<City> = listOf(city)): WidgetUpdateRunner = WidgetUpdateRunner(
        cityRepository = FakeCityRepository(cities),
        weatherRepository = weather,
        unitRepository = FakeTemperatureUnitRepository(),
        snapshotRepository = store,
        refresher = refresher,
        clock = Clock.fixed(now, zone),
        adviceEngine = WeatherAdviceEngine()
    )

    private fun forecast(): Forecast = Forecast(
        city = city,
        currentWeather = CurrentWeather(
            temperatureCelsius = 18,
            condition = WeatherCondition.CLEAR,
            windSpeedKmh = 8,
            humidityPercent = 50,
            isDay = true
        ),
        hourlyForecast = emptyList(),
        dailyForecast = listOf(DailyForecast(0, 10, 20, WeatherCondition.CLEAR)),
        utcOffsetSeconds = 3 * 3600
    )

    @Test
    fun `sehir yokken calismaz`() = runTest {
        val result = runner(cities = emptyList()).run()

        assertEquals(WidgetUpdateRunner.Outcome.SKIPPED, result)
        coVerify(exactly = 0) { weather.forecast(any()) }
        assertEquals(0, refresher.count)
        assertNull(store.stored)
    }

    @Test
    fun `ag hatasinda retry ve eski snapshot korunur`() = runTest {
        coEvery { weather.forecast(any()) } returns AppResult.Failure(AppError.Network)

        val result = runner().run()

        assertEquals(WidgetUpdateRunner.Outcome.RETRY, result)
        assertEquals(0, refresher.count)
        assertNull(store.stored)
    }

    @Test
    fun `kota hatasinda retry yok`() = runTest {
        coEvery { weather.forecast(any()) } returns AppResult.Failure(AppError.Server(429))

        assertEquals(WidgetUpdateRunner.Outcome.SKIPPED, runner().run())
    }

    @Test
    fun `basarida snapshot yazilir ve widget guncellenir`() = runTest {
        coEvery { weather.forecast(any()) } returns AppResult.Success(forecast())

        val result = runner().run()

        assertEquals(WidgetUpdateRunner.Outcome.SUCCESS, result)
        assertEquals("İstanbul", store.stored?.cityName)
        assertEquals(18, store.stored?.temperatureCelsius)
        assertEquals(1, refresher.count)
    }

    @Test
    fun `konum sehri varsa o kullanilir`() = runTest {
        coEvery { weather.forecast(any()) } returns AppResult.Success(forecast())

        runner(cities = listOf(city, locationCity)).run()

        coVerify(exactly = 1) { weather.forecast(locationCity) }
    }

    private class FakeSnapshotRepository : WidgetSnapshotRepository {
        var stored: WidgetSnapshot? = null

        override suspend fun read(): WidgetSnapshot? = stored

        override suspend fun write(snapshot: WidgetSnapshot) {
            stored = snapshot
        }
    }

    private class RecordingRefresher : WidgetRefresher {
        var count = 0

        override suspend fun refresh() {
            count++
        }
    }
}

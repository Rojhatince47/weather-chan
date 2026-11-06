package io.github.rojhatince47.weatherchan.feature.notifications.worker

import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.city.testing.FakeCityRepository
import io.github.rojhatince47.weatherchan.feature.notifications.domain.NotificationPlanner
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationHistory
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.PlannedNotification
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationHistoryStore
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationPermissionChecker
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationPresenter
import io.github.rojhatince47.weatherchan.feature.notifications.testing.FakeNotificationSettingsRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.WeatherAdviceEngine
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCheckRunnerTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val now = Instant.parse("2026-11-06T12:00:00Z") // 15:00 yerel
    private val city = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
    private val locationCity = City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85), isLocation = true)

    private val weather = mockk<WeatherRepository>(relaxed = true)
    private val presenter = RecordingPresenter()
    private val historyStore = FakeHistoryStore()

    private fun runner(
        cities: List<City> = listOf(city),
        settings: NotificationSettings = NotificationSettings(),
        canNotify: Boolean = true
    ): NotificationCheckRunner = NotificationCheckRunner(
        cityRepository = FakeCityRepository(cities),
        weatherRepository = weather,
        settingsRepository = FakeNotificationSettingsRepository(settings),
        historyStore = historyStore,
        planner = NotificationPlanner(WeatherAdviceEngine()),
        presenter = presenter,
        permissionChecker = NotificationPermissionChecker { canNotify },
        clock = Clock.fixed(now, zone)
    )

    private fun stormForecast(): Forecast = Forecast(
        city = city,
        currentWeather = CurrentWeather(
            temperatureCelsius = 15,
            condition = WeatherCondition.THUNDERSTORM,
            windSpeedKmh = 20,
            humidityPercent = 80,
            isDay = true
        ),
        hourlyForecast = emptyList(),
        dailyForecast = listOf(DailyForecast(0, 10, 18, WeatherCondition.THUNDERSTORM))
    )

    @Test
    fun `master kapaliyken fetch olmaz`() = runTest {
        val runner = runner(settings = NotificationSettings(masterEnabled = false))

        assertEquals(NotificationCheckRunner.CheckOutcome.SKIPPED, runner.run())
        coVerify(exactly = 0) { weather.forecast(any()) }
    }

    @Test
    fun `sehir yokken basari doner`() = runTest {
        val runner = runner(cities = emptyList())

        assertEquals(NotificationCheckRunner.CheckOutcome.SKIPPED, runner.run())
        coVerify(exactly = 0) { weather.forecast(any()) }
    }

    @Test
    fun `izin yokken post olmaz`() = runTest {
        coEvery { weather.forecast(any()) } returns AppResult.Success(stormForecast())
        val runner = runner(canNotify = false)

        assertEquals(NotificationCheckRunner.CheckOutcome.SKIPPED, runner.run())
        assertTrue(presenter.posted.isEmpty())
    }

    @Test
    fun `fetch hatasinda retry`() = runTest {
        coEvery { weather.forecast(any()) } returns AppResult.Failure(AppError.Network)
        val runner = runner()

        assertEquals(NotificationCheckRunner.CheckOutcome.RETRY, runner.run())
        assertTrue(presenter.posted.isEmpty())
    }

    @Test
    fun `kota hatasinda retry yok`() = runTest {
        coEvery { weather.forecast(any()) } returns AppResult.Failure(AppError.Server(429))
        val runner = runner()

        assertEquals(NotificationCheckRunner.CheckOutcome.SKIPPED, runner.run())
        assertTrue(presenter.posted.isEmpty())
    }

    @Test
    fun `gonderilemeyen bildirim gecmise yazilmaz`() = runTest {
        coEvery { weather.forecast(any()) } returns AppResult.Success(stormForecast())
        presenter.accept = false
        val runner = runner()

        assertEquals(NotificationCheckRunner.CheckOutcome.SUCCESS, runner.run())
        assertEquals(0, historyStore.stored.totalToday())
    }

    @Test
    fun `planlanan bildirimler post edilir ve gecmis yazilir`() = runTest {
        coEvery { weather.forecast(any()) } returns AppResult.Success(stormForecast())
        val runner = runner()

        assertEquals(NotificationCheckRunner.CheckOutcome.SUCCESS, runner.run())

        assertTrue(presenter.posted.any { it.type == NotificationType.SEVERE_WEATHER })
        assertEquals(1, historyStore.stored.sentCounts.getValue(NotificationType.SEVERE_WEATHER))
    }

    @Test
    fun `konum sehri varsa o kullanilir`() = runTest {
        coEvery { weather.forecast(any()) } returns AppResult.Success(stormForecast())
        val runner = runner(cities = listOf(city, locationCity))

        runner.run()

        coVerify(exactly = 1) { weather.forecast(locationCity) }
    }

    private class RecordingPresenter : NotificationPresenter {
        var accept = true
        val posted = mutableListOf<PlannedNotification>()

        override fun post(notification: PlannedNotification, city: City): Boolean {
            if (!accept) return false
            posted += notification
            return true
        }
    }

    private class FakeHistoryStore : NotificationHistoryStore {
        var stored: NotificationHistory = NotificationHistory(LocalDate.of(2026, 11, 6))

        override suspend fun read(today: LocalDate): NotificationHistory = stored.forDay(today)

        override suspend fun write(history: NotificationHistory) {
            stored = history
        }
    }
}

package io.github.rojhatince47.weatherchan.feature.notifications.domain

import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationHistory
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationPayload
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.PlannerInput
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.SevereReason
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.WeatherAdviceEngine
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.HourlyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPlannerTest {

    private val planner = NotificationPlanner(WeatherAdviceEngine())
    private val city = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
    private val zone = ZoneId.of("Europe/Istanbul")

    // --- yardımcılar ---

    private fun forecast(
        condition: WeatherCondition = WeatherCondition.CLEAR,
        temperatureCelsius: Int = 20,
        windSpeedKmh: Int = 5,
        isDay: Boolean = true,
        hourly: List<HourlyForecast> = emptyList(),
        daily: List<DailyForecast> = listOf(
            DailyForecast(0, 12, 22, condition)
        )
    ): Forecast = Forecast(
        city = city,
        currentWeather = CurrentWeather(
            temperatureCelsius = temperatureCelsius,
            condition = condition,
            windSpeedKmh = windSpeedKmh,
            humidityPercent = 50,
            isDay = isDay
        ),
        hourlyForecast = hourly,
        dailyForecast = daily
    )

    private fun hour(
        localDateTime: LocalDateTime,
        condition: WeatherCondition = WeatherCondition.CLEAR,
        precipitation: Int = 0
    ): HourlyForecast = HourlyForecast(
        localDateTime = localDateTime,
        hourOfDay = localDateTime.hour,
        temperatureCelsius = 20,
        condition = condition,
        precipitationProbabilityPercent = precipitation
    )

    private fun settings(
        master: Boolean = true,
        summaryTime: LocalTime = LocalTime.of(8, 0),
        enabled: Map<NotificationType, Boolean> = NotificationType.entries.associateWith { true }
    ): NotificationSettings = NotificationSettings(master, enabled, summaryTime)

    private fun input(
        localNow: LocalDateTime,
        cityNow: LocalDateTime = localNow,
        settings: NotificationSettings = settings(),
        history: NotificationHistory = NotificationHistory(localNow.toLocalDate()),
        forecast: Forecast = forecast()
    ): PlannerInput = PlannerInput(
        now = localNow.atZone(zone).toInstant(),
        localNow = localNow,
        cityNow = cityNow,
        settings = settings,
        history = history,
        forecast = forecast
    )

    private fun NotificationHistory.withCount(type: NotificationType, count: Int): NotificationHistory =
        copy(sentCounts = sentCounts + (type to count))

    private fun NotificationHistory.withLast(type: NotificationType, at: Instant): NotificationHistory =
        copy(lastSentAt = lastSentAt + (type to at))

    // --- testler ---

    @Test
    fun `master kapaliyken cikti bos`() {
        val now = LocalDateTime.of(2026, 11, 6, 15, 0)
        val output = planner.plan(
            input(
                localNow = now,
                settings = settings(master = false),
                forecast = forecast(condition = WeatherCondition.THUNDERSTORM)
            )
        )

        assertTrue(output.notifications.isEmpty())
    }

    @Test
    fun `sessiz saatte yalniz siddetli gecer`() {
        val quietNow = LocalDateTime.of(2026, 11, 6, 23, 30)
        val settings = settings(summaryTime = LocalTime.of(23, 0))

        val quiet = planner.plan(input(localNow = quietNow, settings = settings))
        assertTrue(quiet.notifications.isEmpty())

        val storm = planner.plan(
            input(
                localNow = quietNow,
                settings = settings,
                forecast = forecast(condition = WeatherCondition.THUNDERSTORM)
            )
        )
        assertEquals(listOf(NotificationType.SEVERE_WEATHER), storm.notifications.map { it.type })
    }

    @Test
    fun `firtinada STORM uyarisi`() {
        val output = planner.plan(
            input(
                localNow = LocalDateTime.of(2026, 11, 6, 15, 0),
                forecast = forecast(condition = WeatherCondition.THUNDERSTORM)
            )
        )

        val severe = output.notifications.single { it.type == NotificationType.SEVERE_WEATHER }
        assertEquals(SevereReason.STORM, (severe.payload as NotificationPayload.Severe).reason)
    }

    @Test
    fun `siddetli gunde en fazla 2 ve 6 saat bekleme`() {
        val now = LocalDateTime.of(2026, 11, 6, 15, 0)
        val storm = forecast(condition = WeatherCondition.THUNDERSTORM)
        val summaryOff = settings(
            enabled = NotificationType.entries.associateWith { it != NotificationType.DAILY_SUMMARY }
        )

        val tooSoon = NotificationHistory(now.toLocalDate())
            .withCount(NotificationType.SEVERE_WEATHER, 1)
            .withLast(NotificationType.SEVERE_WEATHER, now.minusHours(5).atZone(zone).toInstant())
        assertTrue(
            planner.plan(input(now, settings = summaryOff, history = tooSoon, forecast = storm))
                .notifications.isEmpty()
        )

        val cooldownOver = tooSoon.withLast(
            NotificationType.SEVERE_WEATHER,
            now.minusHours(7).atZone(zone).toInstant()
        )
        assertEquals(
            1,
            planner.plan(input(now, settings = summaryOff, history = cooldownOver, forecast = storm))
                .notifications.count { it.type == NotificationType.SEVERE_WEATHER }
        )

        val twoSent = NotificationHistory(now.toLocalDate())
            .withCount(NotificationType.SEVERE_WEATHER, 2)
        assertTrue(
            planner.plan(input(now, settings = summaryOff, history = twoSent, forecast = storm))
                .notifications.isEmpty()
        )
    }

    @Test
    fun `yagmur yaklasirken uyari`() {
        val now = LocalDateTime.of(2026, 11, 6, 10, 0)
        val output = planner.plan(
            input(
                localNow = now,
                forecast = forecast(
                    hourly = listOf(hour(now.plusHours(1), WeatherCondition.RAIN, 70))
                )
            )
        )

        val rain = output.notifications.single { it.type == NotificationType.RAIN_SOON }
        assertTrue(rain.payload is NotificationPayload.RainSoon)
    }

    @Test
    fun `yagarken yagmur uyarisi yok`() {
        val now = LocalDateTime.of(2026, 11, 6, 10, 0)
        val output = planner.plan(
            input(
                localNow = now,
                forecast = forecast(
                    condition = WeatherCondition.RAIN,
                    hourly = listOf(hour(now.plusHours(1), WeatherCondition.RAIN, 90))
                )
            )
        )

        assertTrue(output.notifications.none { it.type == NotificationType.RAIN_SOON })
    }

    @Test
    fun `yagmur uyarisinda gunde 1`() {
        val now = LocalDateTime.of(2026, 11, 6, 10, 0)
        val history = NotificationHistory(now.toLocalDate())
            .withCount(NotificationType.RAIN_SOON, 1)
        val output = planner.plan(
            input(
                localNow = now,
                history = history,
                forecast = forecast(
                    hourly = listOf(hour(now.plusHours(1), WeatherCondition.RAIN, 70))
                )
            )
        )

        assertTrue(output.notifications.none { it.type == NotificationType.RAIN_SOON })
    }

    @Test
    fun `ozet saatinde bir kez`() {
        val now = LocalDateTime.of(2026, 11, 6, 8, 5)
        val first = planner.plan(input(localNow = now))
        assertEquals(listOf(NotificationType.DAILY_SUMMARY), first.notifications.map { it.type })

        val second = planner.plan(input(localNow = now.plusMinutes(30), history = first.history))
        assertTrue(second.notifications.isEmpty())
    }

    @Test
    fun `ozet saati gelmeden yok`() {
        val output = planner.plan(
            input(
                localNow = LocalDateTime.of(2026, 11, 6, 8, 30),
                settings = settings(summaryTime = LocalTime.of(9, 0))
            )
        )

        assertTrue(output.notifications.isEmpty())
    }

    @Test
    fun `ozet saati sessiz saatteyse 08 00 de gecer`() {
        val output = planner.plan(
            input(
                localNow = LocalDateTime.of(2026, 11, 6, 8, 0),
                settings = settings(summaryTime = LocalTime.of(7, 0))
            )
        )

        assertEquals(listOf(NotificationType.DAILY_SUMMARY), output.notifications.map { it.type })
    }

    @Test
    fun `karakter baska uyari yoksa ve ozetten sonra cikar`() {
        val now = LocalDateTime.of(2026, 11, 6, 15, 0)

        // Aynı turda özet çıkıyorsa karakter eklenmez; üst üste binmez.
        val first = planner.plan(input(localNow = now))
        assertEquals(listOf(NotificationType.DAILY_SUMMARY), first.notifications.map { it.type })

        // Özet zaten gitmişse karakter tek başına çıkar.
        val second = planner.plan(input(localNow = now.plusMinutes(30), history = first.history))
        assertEquals(listOf(NotificationType.COMPANION), second.notifications.map { it.type })

        // Bugün şiddetli/yağmur uyarısı gittiyse karakter susar.
        val afterAlert = first.history.withCount(NotificationType.SEVERE_WEATHER, 1)
        assertTrue(
            planner.plan(input(now.plusHours(1), history = afterAlert))
                .notifications.none { it.type == NotificationType.COMPANION }
        )
    }

    @Test
    fun `karakter gunde 1 ve 10-20 arasi`() {
        val summaryOff = settings(
            enabled = NotificationType.entries.associateWith { it != NotificationType.DAILY_SUMMARY }
        )
        val sent = LocalDateTime.of(2026, 11, 6, 15, 0)
        val alreadySent = NotificationHistory(sent.toLocalDate())
            .withCount(NotificationType.COMPANION, 1)
        assertTrue(
            planner.plan(input(sent, settings = summaryOff, history = alreadySent))
                .notifications.isEmpty()
        )

        assertTrue(
            planner.plan(input(LocalDateTime.of(2026, 11, 6, 9, 0), settings = summaryOff))
                .notifications.isEmpty()
        )
        assertTrue(
            planner.plan(input(LocalDateTime.of(2026, 11, 6, 21, 0), settings = summaryOff))
                .notifications.isEmpty()
        )
    }

    @Test
    fun `gun donunce sayac sifirlanir`() {
        val now = LocalDateTime.of(2026, 11, 7, 15, 0)
        val yesterday = NotificationHistory(LocalDate.of(2026, 11, 6))
            .withCount(NotificationType.SEVERE_WEATHER, 2)
            .withCount(NotificationType.COMPANION, 1)

        val output = planner.plan(input(localNow = now, history = yesterday))
        assertEquals(now.toLocalDate(), output.history.day)
        assertTrue(output.notifications.isNotEmpty())
    }

    @Test
    fun `tur kapaliyse o tur cikmaz`() {
        val now = LocalDateTime.of(2026, 11, 6, 15, 0)
        val output = planner.plan(
            input(
                localNow = now,
                settings = settings(
                    enabled = NotificationType.entries.associateWith { false }
                ),
                forecast = forecast(condition = WeatherCondition.THUNDERSTORM)
            )
        )

        assertTrue(output.notifications.isEmpty())
    }

    @Test
    fun `gunluk tavan 4 u gecmez`() {
        val now = LocalDateTime.of(2026, 11, 6, 10, 5)
        val history = NotificationHistory(now.toLocalDate())
            .withCount(NotificationType.SEVERE_WEATHER, 1)
            .withLast(NotificationType.SEVERE_WEATHER, now.minusHours(7).atZone(zone).toInstant())
        val output = planner.plan(
            input(
                localNow = now,
                history = history,
                forecast = forecast(
                    windSpeedKmh = 65,
                    hourly = listOf(hour(now.plusHours(1), WeatherCondition.RAIN, 80))
                )
            )
        )

        assertTrue(output.notifications.size <= 4)
        assertTrue(output.notifications.none { it.type == NotificationType.COMPANION })
        assertEquals(
            listOf(
                NotificationType.SEVERE_WEATHER,
                NotificationType.RAIN_SOON,
                NotificationType.DAILY_SUMMARY
            ),
            output.notifications.map { it.type }
        )
        assertTrue(output.history.sentCounts.values.sum() <= 4)
    }

    @Test
    fun `sehir saat dilimi farkliysa yagmur penceresi sehir saatine gore`() {
        val deviceNow = LocalDateTime.of(2026, 11, 6, 10, 0)
        val cityNow = LocalDateTime.of(2026, 11, 6, 3, 0)

        val soon = planner.plan(
            input(
                localNow = deviceNow,
                cityNow = cityNow,
                forecast = forecast(
                    hourly = listOf(hour(cityNow.plusHours(1), WeatherCondition.RAIN, 70))
                )
            )
        )
        assertTrue(soon.notifications.any { it.type == NotificationType.RAIN_SOON })

        val later = planner.plan(
            input(
                localNow = deviceNow,
                cityNow = cityNow,
                forecast = forecast(
                    hourly = listOf(hour(cityNow.plusHours(8), WeatherCondition.RAIN, 70))
                )
            )
        )
        assertTrue(later.notifications.none { it.type == NotificationType.RAIN_SOON })
    }

    @Test
    fun `kar yaklasirken yagmur uyarisi yok`() {
        val now = LocalDateTime.of(2026, 11, 6, 10, 0)
        val output = planner.plan(
            input(
                localNow = now,
                forecast = forecast(
                    hourly = listOf(hour(now.plusHours(1), WeatherCondition.SNOW, 90))
                )
            )
        )

        assertTrue(output.notifications.none { it.type == NotificationType.RAIN_SOON })
    }

    @Test
    fun `dunun sayaclari bugunu engellemez`() {
        val now = LocalDateTime.of(2026, 11, 7, 15, 0)
        val yesterday = NotificationHistory(LocalDate.of(2026, 11, 6))
            .withCount(NotificationType.SEVERE_WEATHER, 2)
            .withCount(NotificationType.COMPANION, 1)

        val output = planner.plan(
            input(
                localNow = now,
                history = yesterday,
                forecast = forecast(condition = WeatherCondition.THUNDERSTORM)
            )
        )

        assertTrue(output.notifications.any { it.type == NotificationType.SEVERE_WEATHER })
    }

    @Test
    fun `gunluk tavan dort bildirimde kapanir`() {
        val now = LocalDateTime.of(2026, 11, 6, 15, 0)
        val full = NotificationHistory(now.toLocalDate())
            .withCount(NotificationType.SEVERE_WEATHER, 1)
            .withCount(NotificationType.RAIN_SOON, 1)
            .withCount(NotificationType.DAILY_SUMMARY, 1)
            .withCount(NotificationType.COMPANION, 1)
            .withLast(
                NotificationType.SEVERE_WEATHER,
                now.minusHours(7).atZone(zone).toInstant()
            )

        val output = planner.plan(
            input(
                localNow = now,
                history = full,
                forecast = forecast(condition = WeatherCondition.THUNDERSTORM)
            )
        )

        assertTrue(output.notifications.isEmpty())
        assertEquals(4, output.history.totalToday())
    }

    @Test
    fun `gece yarisina ayarli ozet 08 00 de gider`() {
        val output = planner.plan(
            input(
                localNow = LocalDateTime.of(2026, 11, 6, 8, 5),
                settings = settings(summaryTime = LocalTime.of(23, 30))
            )
        )

        assertEquals(listOf(NotificationType.DAILY_SUMMARY), output.notifications.map { it.type })
    }

    @Test
    fun `siddetli nedenleri sirayla secilir`() {
        val now = LocalDateTime.of(2026, 11, 6, 15, 0)

        val snow = planner.plan(
            input(now, forecast = forecast(condition = WeatherCondition.SNOW))
        ).notifications.single { it.type == NotificationType.SEVERE_WEATHER }
        assertEquals(SevereReason.SNOW, (snow.payload as NotificationPayload.Severe).reason)

        val wind = planner.plan(
            input(now, forecast = forecast(windSpeedKmh = 65))
        ).notifications.single { it.type == NotificationType.SEVERE_WEATHER }
        assertEquals(SevereReason.WIND, (wind.payload as NotificationPayload.Severe).reason)

        val cold = planner.plan(
            input(now, forecast = forecast(temperatureCelsius = -12))
        ).notifications.single { it.type == NotificationType.SEVERE_WEATHER }
        assertEquals(SevereReason.COLD, (cold.payload as NotificationPayload.Severe).reason)

        val hot = planner.plan(
            input(now, forecast = forecast(temperatureCelsius = 41))
        ).notifications.single { it.type == NotificationType.SEVERE_WEATHER }
        assertEquals(SevereReason.HOT, (hot.payload as NotificationPayload.Severe).reason)
    }

    @Test
    fun `sicaklik ve ruzgar sinirda uyari vermez`() {
        val now = LocalDateTime.of(2026, 11, 6, 15, 0)

        assertTrue(
            planner.plan(input(now, forecast = forecast(temperatureCelsius = -9)))
                .notifications.none { it.type == NotificationType.SEVERE_WEATHER }
        )
        assertTrue(
            planner.plan(input(now, forecast = forecast(windSpeedKmh = 59)))
                .notifications.none { it.type == NotificationType.SEVERE_WEATHER }
        )
    }

    @Test
    fun `bildirim atildiginda gecmis guncellenir`() {
        val now = LocalDateTime.of(2026, 11, 6, 15, 0)
        val output = planner.plan(
            input(now, forecast = forecast(condition = WeatherCondition.THUNDERSTORM))
        )

        assertEquals(1, output.history.sentCounts.getValue(NotificationType.SEVERE_WEATHER))
        assertEquals(
            now.atZone(zone).toInstant(),
            output.history.lastSentAt.getValue(NotificationType.SEVERE_WEATHER)
        )
    }

    @Test
    fun `siddetli uyari sessiz saatte de gecmis yazar`() {
        val now = LocalDateTime.of(2026, 11, 6, 2, 0)
        val output = planner.plan(
            input(now, forecast = forecast(condition = WeatherCondition.THUNDERSTORM))
        )

        assertTrue(output.notifications.isNotEmpty())
        assertEquals(
            now.atZone(zone).toInstant(),
            output.history.lastSentAt.getValue(NotificationType.SEVERE_WEATHER)
        )
    }
}

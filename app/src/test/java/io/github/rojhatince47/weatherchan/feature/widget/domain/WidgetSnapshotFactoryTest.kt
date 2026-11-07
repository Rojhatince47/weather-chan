package io.github.rojhatince47.weatherchan.feature.widget.domain

import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceCategory
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.HourlyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import java.time.Instant
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSnapshotFactoryTest {

    private val city = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
    private val now = Instant.parse("2026-11-06T12:00:00Z") // +03 → 15:00

    private fun forecast(
        condition: WeatherCondition = WeatherCondition.CLEAR,
        temperature: Int = 18,
        isDay: Boolean = true,
        utcOffsetSeconds: Int = 3 * 3600,
        hourly: List<HourlyForecast> = emptyList()
    ): Forecast = Forecast(
        city = city,
        currentWeather = CurrentWeather(
            temperatureCelsius = temperature,
            condition = condition,
            windSpeedKmh = 8,
            humidityPercent = 50,
            isDay = isDay
        ),
        hourlyForecast = hourly,
        dailyForecast = listOf(DailyForecast(0, 10, 20, condition)),
        utcOffsetSeconds = utcOffsetSeconds
    )

    private fun hour(hourOfDay: Int, temperature: Int = 15): HourlyForecast = HourlyForecast(
        localDateTime = LocalDateTime.of(2026, 11, 6, hourOfDay, 0),
        hourOfDay = hourOfDay,
        temperatureCelsius = temperature,
        condition = WeatherCondition.CLOUDY,
        precipitationProbabilityPercent = 0
    )

    @Test
    fun `anlik durum birim ve poz snapshot a girer`() {
        val snapshot = WidgetSnapshotFactory.create(
            city = city,
            forecast = forecast(temperature = 18, isDay = false),
            unit = TemperatureUnit.FAHRENHEIT,
            pose = CompanionPose.CHEERFUL,
            adviceCategory = AdviceCategory.CLEAR,
            adviceSeed = 7,
            now = now
        )

        assertEquals("İstanbul", snapshot.cityName)
        assertEquals(18, snapshot.temperatureCelsius)
        assertEquals("FAHRENHEIT", snapshot.unit)
        assertEquals("CLEAR", snapshot.condition)
        assertEquals(false, snapshot.isDay)
        assertEquals("CHEERFUL", snapshot.pose)
        assertEquals(now.toEpochMilli(), snapshot.updatedAtEpochMillis)
    }

    @Test
    fun `saatlikler sehir saatine gore secilir`() {
        // Şehirde 15:00; 15:00 "sonra" değildir, 16-19 alınır.
        val snapshot = WidgetSnapshotFactory.create(
            city = city,
            forecast = forecast(
                hourly = listOf(
                    hour(14),
                    hour(15),
                    hour(16, 16),
                    hour(17, 17),
                    hour(18, 18),
                    hour(19, 19)
                )
            ),
            unit = TemperatureUnit.CELSIUS,
            pose = CompanionPose.CALM,
            adviceCategory = AdviceCategory.CLOUDY,
            adviceSeed = 3,
            now = now
        )

        assertEquals(listOf(16, 17, 18, 19), snapshot.hourly.map { it.hourOfDay })
        assertEquals(listOf(16, 17, 18, 19), snapshot.hourly.map { it.temperatureCelsius })
    }

    @Test
    fun `saatlik yoksa liste bos`() {
        val snapshot = WidgetSnapshotFactory.create(
            city = city,
            forecast = forecast(),
            unit = TemperatureUnit.CELSIUS,
            pose = CompanionPose.CALM,
            adviceCategory = AdviceCategory.CLOUDY,
            adviceSeed = 3,
            now = now
        )

        assertTrue(snapshot.hourly.isEmpty())
    }
}

package io.github.rojhatince47.weatherchan.feature.widget.domain

import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceCategory
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Tahmin + birim + poz → widget anlık görüntüsü. Saf fonksiyon; Android'e
 * bağımlı değildir, bu yüzden birim testiyle sabitlenir.
 */
object WidgetSnapshotFactory {

    fun create(
        city: City,
        forecast: Forecast,
        unit: TemperatureUnit,
        pose: CompanionPose,
        adviceCategory: AdviceCategory,
        adviceSeed: Int,
        now: Instant,
        hourlyCount: Int = DEFAULT_HOURLY_COUNT
    ): WidgetSnapshot {
        // Saatlik damgalar şehrin dilimindedir; "sonraki saatler" ona göre
        // seçilir (cihaz dilimi farklı olabilir).
        val cityNow = LocalDateTime.ofInstant(
            now,
            ZoneOffset.ofTotalSeconds(forecast.utcOffsetSeconds)
        )
        val hourly = forecast.hourlyForecast
            .filter { it.localDateTime.isAfter(cityNow) }
            .take(hourlyCount)
            .map {
                WidgetHourSnapshot(
                    hourOfDay = it.hourOfDay,
                    temperatureCelsius = it.temperatureCelsius,
                    condition = it.condition.name
                )
            }

        return WidgetSnapshot(
            cityName = city.name,
            temperatureCelsius = forecast.currentWeather.temperatureCelsius,
            unit = unit.name,
            condition = forecast.currentWeather.condition.name,
            isDay = forecast.currentWeather.isDay,
            pose = pose.name,
            hourly = hourly,
            updatedAtEpochMillis = now.toEpochMilli(),
            adviceCategory = adviceCategory.name,
            adviceSeed = adviceSeed
        )
    }

    private const val DEFAULT_HOURLY_COUNT = 4
}

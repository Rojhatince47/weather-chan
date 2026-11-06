package io.github.rojhatince47.weatherchan.feature.widget.worker

import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceContext
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.HourlyOutlook
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.WeatherAdviceEngine
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import io.github.rojhatince47.weatherchan.feature.widget.domain.WidgetSnapshotFactory
import io.github.rojhatince47.weatherchan.feature.widget.domain.WidgetSnapshotRepository
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Widget çizimini tetikleyen port; gerçek uygulaması Glance'tır. */
fun interface WidgetRefresher {
    suspend fun refresh()
}

/**
 * Widget güncellemesinin saf iş akışı. Hedef şehir bildirimlerle aynı kuraldır
 * (konum varsa o, yoksa ilk şehir); hata durumunda eski anlık görüntü korunur.
 */
class WidgetUpdateRunner @Inject constructor(
    private val cityRepository: CityRepository,
    private val weatherRepository: WeatherRepository,
    private val unitRepository: TemperatureUnitRepository,
    private val snapshotRepository: WidgetSnapshotRepository,
    private val refresher: WidgetRefresher,
    private val clock: Clock,
    private val adviceEngine: WeatherAdviceEngine
) {

    suspend fun run(): Outcome {
        val city = targetCity() ?: return Outcome.SKIPPED

        val forecast = when (val result = weatherRepository.forecast(city)) {
            is AppResult.Success -> result.data
            is AppResult.Failure ->
                return if (result.error.isTransient()) Outcome.RETRY else Outcome.SKIPPED
        }

        val now = clock.instant()
        val cityNow = LocalDateTime.ofInstant(
            now,
            ZoneOffset.ofTotalSeconds(forecast.utcOffsetSeconds)
        )
        val advice = adviceEngine.advise(
            AdviceContext(
                cityId = city.id,
                temperatureCelsius = forecast.currentWeather.temperatureCelsius,
                condition = forecast.currentWeather.condition,
                isDay = forecast.currentWeather.isDay,
                windSpeedKmh = forecast.currentWeather.windSpeedKmh,
                upcomingHours = forecast.hourlyForecast
                    .filter { it.localDateTime.isAfter(cityNow) }
                    .take(2)
                    .map { HourlyOutlook(it.precipitationProbabilityPercent, it.condition) },
                epochDay = cityNow.toLocalDate().toEpochDay(),
                hourOfDay = cityNow.hour
            )
        )

        snapshotRepository.write(
            WidgetSnapshotFactory.create(
                city = city,
                forecast = forecast,
                unit = unitRepository.unit.value,
                pose = advice.pose,
                now = now
            )
        )
        refresher.refresh()
        return Outcome.SUCCESS
    }

    private suspend fun targetCity(): City? {
        val cities = cityRepository.cities.first()
        return cities.firstOrNull { it.isLocation } ?: cities.firstOrNull()
    }

    private fun AppError.isTransient(): Boolean = when (this) {
        AppError.Network, is AppError.Unknown -> true
        is AppError.Server -> code >= 500
        AppError.NotFound, AppError.Parse -> false
    }

    enum class Outcome { SUCCESS, RETRY, SKIPPED }
}

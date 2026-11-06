package io.github.rojhatince47.weatherchan.feature.notifications.worker

import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.notifications.domain.NotificationPlanner
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.PlannerInput
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationHistoryStore
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationPermissionChecker
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationPresenter
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationSettingsRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Tek bir bildirim kontrolü. WorkManager işçisi yalnız bu sınıfı çağırır;
 * böylece karar akışı Android'e bağlı olmadan test edilebilir.
 *
 * Şehir kuralı: konum şehri varsa o, yoksa listedeki ilk şehir. Liste boşsa,
 * anahtar kapalıysa veya izin yoksa sessizce çıkılır (bildirim yığılmaz).
 */
class NotificationCheckRunner @Inject constructor(
    private val cityRepository: CityRepository,
    private val weatherRepository: WeatherRepository,
    private val settingsRepository: NotificationSettingsRepository,
    private val historyStore: NotificationHistoryStore,
    private val planner: NotificationPlanner,
    private val presenter: NotificationPresenter,
    private val permissionChecker: NotificationPermissionChecker,
    private val clock: Clock
) {

    suspend fun run(): CheckOutcome {
        val settings = settingsRepository.read()
        if (!settings.masterEnabled) return CheckOutcome.SKIPPED

        val city = targetCity() ?: return CheckOutcome.SKIPPED
        if (!permissionChecker.canNotify()) return CheckOutcome.SKIPPED

        val forecast = when (val result = weatherRepository.forecast(city)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return CheckOutcome.RETRY
        }

        val now = clock.instant()
        val localNow = LocalDateTime.ofInstant(now, clock.zone)
        val output = planner.plan(
            PlannerInput(
                now = now,
                localNow = localNow,
                settings = settings,
                history = historyStore.read(localNow.toLocalDate()),
                forecast = forecast
            )
        )

        output.notifications.forEach { presenter.post(it, city) }
        if (output.notifications.isNotEmpty()) historyStore.write(output.history)
        return CheckOutcome.SUCCESS
    }

    private suspend fun targetCity(): City? {
        val cities = cityRepository.cities.first()
        return cities.firstOrNull { it.isLocation } ?: cities.firstOrNull()
    }

    enum class CheckOutcome {
        SUCCESS,
        RETRY,
        SKIPPED
    }
}

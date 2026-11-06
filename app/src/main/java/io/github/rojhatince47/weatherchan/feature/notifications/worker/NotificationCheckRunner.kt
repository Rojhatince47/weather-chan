package io.github.rojhatince47.weatherchan.feature.notifications.worker

import io.github.rojhatince47.weatherchan.core.common.error.AppError
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
import java.time.ZoneOffset
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
            is AppResult.Failure ->
                return if (result.error.isTransient()) CheckOutcome.RETRY else CheckOutcome.SKIPPED
        }

        val now = clock.instant()
        val localNow = LocalDateTime.ofInstant(now, clock.zone)
        val cityNow = LocalDateTime.ofInstant(
            now,
            ZoneOffset.ofTotalSeconds(forecast.utcOffsetSeconds)
        )
        val history = historyStore.read(localNow.toLocalDate())
        val output = planner.plan(
            PlannerInput(
                now = now,
                localNow = localNow,
                cityNow = cityNow,
                settings = settings,
                history = history,
                forecast = forecast
            )
        )

        // Yalnız gerçekten gösterilen bildirimler geçmişe yazılır; izin/kanal
        // kapalıysa günlük hak yanmaz.
        val posted = output.notifications.filter { presenter.post(it, city) }
        if (posted.isEmpty()) return CheckOutcome.SUCCESS

        val updated = posted.fold(history.forDay(localNow.toLocalDate())) { acc, notification ->
            acc.record(notification.type, now)
        }
        historyStore.write(updated)
        return CheckOutcome.SUCCESS
    }

    /**
     * Yalnız geçici hatalar tekrar denenir. Kota (429), bulunamadı ve çözümleme
     * hataları kalıcıdır; retry kotayı düzeltmez, pili harcar.
     */
    private fun AppError.isTransient(): Boolean = when (this) {
        AppError.Network, is AppError.Unknown -> true
        is AppError.Server -> code >= 500
        AppError.NotFound, AppError.Parse -> false
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

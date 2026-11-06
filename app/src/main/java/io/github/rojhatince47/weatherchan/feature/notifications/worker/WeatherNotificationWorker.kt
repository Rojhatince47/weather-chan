package io.github.rojhatince47.weatherchan.feature.notifications.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 30 dakikalık periyodik kontrolün Android kabuğu. Karar ve gönderim
 * [NotificationCheckRunner] içindedir; burası yalnız sonucu WorkManager
 * diline çevirir.
 */
@HiltWorker
class WeatherNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val runner: NotificationCheckRunner
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = when (runner.run()) {
        NotificationCheckRunner.CheckOutcome.RETRY -> Result.retry()
        NotificationCheckRunner.CheckOutcome.SUCCESS,
        NotificationCheckRunner.CheckOutcome.SKIPPED -> Result.success()
    }
}

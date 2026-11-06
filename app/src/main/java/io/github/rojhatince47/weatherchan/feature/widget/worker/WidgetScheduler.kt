package io.github.rojhatince47.weatherchan.feature.widget.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Widget yenilemesini widget yaşadığı sürece çalıştırır. Widget yokken iş
 * kurulmaz; böylece boşuna ağ/pil harcanmaz.
 */
object WidgetScheduler {
    private const val PERIODIC_WORK = "widget_update"
    private const val IMMEDIATE_WORK = "widget_update_now"
    private const val INTERVAL_MINUTES = 30L

    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun enqueueImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelIfUnused(context: Context, hasWidgets: Boolean) {
        if (!hasWidgets) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        }
    }
}

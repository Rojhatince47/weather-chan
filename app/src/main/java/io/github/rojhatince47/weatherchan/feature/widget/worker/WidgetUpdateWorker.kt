package io.github.rojhatince47.weatherchan.feature.widget.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val runner: WidgetUpdateRunner
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = when (runner.run()) {
        WidgetUpdateRunner.Outcome.RETRY -> Result.retry()
        WidgetUpdateRunner.Outcome.SUCCESS,
        WidgetUpdateRunner.Outcome.SKIPPED -> Result.success()
    }
}

package io.github.rojhatince47.weatherchan

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.rojhatince47.weatherchan.feature.notifications.data.NotificationChannels
import javax.inject.Inject

@HiltAndroidApp
class HavaApplication :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Kanallar uygulama açılışında bir kez kurulur; tekrar kurmak zararsızdır.
        NotificationChannels.create(this)
    }
}

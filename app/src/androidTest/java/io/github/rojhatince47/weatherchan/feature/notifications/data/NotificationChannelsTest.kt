package io.github.rojhatince47.weatherchan.feature.notifications.data

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationChannelsTest {

    @Test
    fun ucBildirimKanaliDogruOnemleKurulur() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(NotificationManager::class.java)

        // Uygulama açılışında kurulmuş olabilir; testin kendi kurulumunu
        // görmesi için önce silinir.
        manager.deleteNotificationChannel(NotificationChannels.ALERTS)
        manager.deleteNotificationChannel(NotificationChannels.SUMMARY)
        manager.deleteNotificationChannel(NotificationChannels.COMPANION)

        NotificationChannels.create(context)

        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            manager.getNotificationChannel(NotificationChannels.ALERTS).importance
        )
        assertNotNull(manager.getNotificationChannel(NotificationChannels.SUMMARY))
        assertNotNull(manager.getNotificationChannel(NotificationChannels.COMPANION))
    }
}

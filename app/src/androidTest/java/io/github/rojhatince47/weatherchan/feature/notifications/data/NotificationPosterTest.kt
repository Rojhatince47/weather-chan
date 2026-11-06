package io.github.rojhatince47.weatherchan.feature.notifications.data

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationPayload
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.PlannedNotification
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.SevereReason
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationPosterTest {

    private val city = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))

    @Test
    fun gonderim_bildirim_yoneticisinde_gorunur() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // İzin yoksa cihazda bildirim görünmez; testi atla (CI cihazı).
        assumeTrue(NotificationManagerCompat.from(context).areNotificationsEnabled())
        NotificationChannels.create(context)

        val poster = NotificationPoster(context, FakeTemperatureUnitRepository())
        poster.post(
            PlannedNotification(
                NotificationType.SEVERE_WEATHER,
                NotificationPayload.Severe(SevereReason.STORM)
            ),
            city
        )

        val manager = context.getSystemService(NotificationManager::class.java)
        assertTrue(
            manager.activeNotifications.any { it.packageName == context.packageName }
        )
        manager.cancelAll()
    }

    @Test
    fun ozet_bildirimi_yer_tutuculari_doldurur() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assumeTrue(NotificationManagerCompat.from(context).areNotificationsEnabled())
        NotificationChannels.create(context)

        val poster = NotificationPoster(context, FakeTemperatureUnitRepository())
        poster.post(
            PlannedNotification(
                NotificationType.DAILY_SUMMARY,
                NotificationPayload.Summary(
                    daily = DailyForecast(0, 5, 15, WeatherCondition.CLEAR),
                    rainChancePercent = 30
                )
            ),
            city
        )

        val manager = context.getSystemService(NotificationManager::class.java)
        val notification = manager.activeNotifications
            .first { it.id == NotificationType.DAILY_SUMMARY.ordinal + 1 }
            .notification
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()

        assertTrue("şehir metinde olmalı: $text", text.contains("İstanbul"))
        assertTrue("sıcaklık birimi metinde olmalı: $text", text.contains("°C"))
        assertTrue("yağmur yüzdesi metinde olmalı: $text", text.contains("30"))
        assertTrue("ham yer tutucu kalmamalı: $text", !text.contains("%%"))
        manager.cancelAll()
    }
}

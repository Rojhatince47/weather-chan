package io.github.rojhatince47.weatherchan.feature.notifications.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import io.github.rojhatince47.weatherchan.R

/**
 * Bildirim kanalları. Uygulama açılışında bir kez kurulur; kanal tanımı
 * sonradan değişse bile Android kullanıcının seçimlerini korur.
 *
 * Üç kanal bilinçli: uyarılar yüksek önemde (baş üstü), özet ve karakter
 * mesajları varsayılan önemde. Ses her kanalda sistem varsayılanıdır.
 */
object NotificationChannels {
    const val ALERTS = "weather_alerts"
    const val SUMMARY = "weather_summary"
    const val COMPANION = "weather_companion"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                channel(context, ALERTS, R.string.notif_channel_alerts, NotificationManager.IMPORTANCE_HIGH),
                channel(context, SUMMARY, R.string.notif_channel_summary, NotificationManager.IMPORTANCE_DEFAULT),
                channel(context, COMPANION, R.string.notif_channel_companion, NotificationManager.IMPORTANCE_DEFAULT)
            )
        )
    }

    private fun channel(context: Context, id: String, @StringRes nameRes: Int, importance: Int): NotificationChannel =
        NotificationChannel(id, context.getString(nameRes), importance)
}

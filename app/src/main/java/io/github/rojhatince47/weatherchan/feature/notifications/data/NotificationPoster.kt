package io.github.rojhatince47.weatherchan.feature.notifications.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.rojhatince47.weatherchan.MainActivity
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.feature.notifications.domain.SummaryTemperatureFormatter
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationPayload
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.PlannedNotification
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationPresenter
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion.textArrayRes
import javax.inject.Inject

/**
 * Planlanan bildirimi Android bildirimine çevirir.
 *
 * Kimlik tür başına sabittir: aynı türden yeni bildirim eskisinin üzerine
 * yazar, bildirim gölü birikmez. Ses kanalın varsayılanıdır (sistem tonu).
 */
class NotificationPoster @Inject constructor(
    @ApplicationContext private val context: Context,
    private val temperatureUnitRepository: TemperatureUnitRepository
) : NotificationPresenter {

    override fun post(notification: PlannedNotification, city: City) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val text = resolveText(notification, city)
        val androidNotification = NotificationCompat.Builder(context, channelOf(notification.type))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(NotificationTexts.titleRes(notification.type)))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tapIntent(notification.type))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(notification.type), androidNotification)
    }

    private fun resolveText(planned: PlannedNotification, city: City): String = when (val payload = planned.payload) {
        is NotificationPayload.Summary -> {
            val unit = temperatureUnitRepository.unit.value
            context.getString(
                R.string.notif_summary,
                city.name,
                SummaryTemperatureFormatter.temperature(payload.daily.maxTemperatureCelsius, unit),
                SummaryTemperatureFormatter.temperature(payload.daily.minTemperatureCelsius, unit),
                payload.rainChancePercent
            )
        }
        is NotificationPayload.Companion -> {
            val lines = context.resources.getStringArray(payload.category.textArrayRes())
            lines.random()
        }
        else -> context.getString(NotificationTexts.textRes(payload))
    }

    private fun channelOf(type: NotificationType): String = when (type) {
        NotificationType.SEVERE_WEATHER,
        NotificationType.RAIN_SOON -> NotificationChannels.ALERTS
        NotificationType.DAILY_SUMMARY -> NotificationChannels.SUMMARY
        NotificationType.COMPANION -> NotificationChannels.COMPANION
    }

    private fun notificationId(type: NotificationType): Int = type.ordinal + 1

    private fun tapIntent(type: NotificationType): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            type.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

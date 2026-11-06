package io.github.rojhatince47.weatherchan.feature.notifications.data

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationPayload
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.SevereReason
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceCategory
import io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion.lineIndex
import io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion.textArrayRes

/**
 * Bildirim başlık ve metin kaynakları. Saf tutulur: kaynak kimliği döner,
 * gerçek metni poster Android kaynaklarından çözer. Karakter mesajı mevcut
 * `advice_*` havuzlarını yeniden kullanır; yeni metin uydurulmaz.
 */
object NotificationTexts {

    fun titleRes(type: NotificationType): Int = when (type) {
        NotificationType.SEVERE_WEATHER -> R.string.notif_title_severe
        NotificationType.RAIN_SOON -> R.string.notif_title_rain
        NotificationType.DAILY_SUMMARY -> R.string.notif_title_summary
        NotificationType.COMPANION -> R.string.notif_title_companion
    }

    fun textRes(payload: NotificationPayload): Int = when (payload) {
        is NotificationPayload.Severe -> when (payload.reason) {
            SevereReason.STORM -> R.string.notif_severe_storm
            SevereReason.SNOW -> R.string.notif_severe_snow
            SevereReason.WIND -> R.string.notif_severe_wind
            SevereReason.COLD -> R.string.notif_severe_cold
            SevereReason.HOT -> R.string.notif_severe_hot
        }
        is NotificationPayload.RainSoon -> R.string.notif_rain_soon
        is NotificationPayload.Summary -> R.string.notif_summary
        is NotificationPayload.Companion -> payload.category.textArrayRes()
    }

    /** Karakter repliği seçimi ana ekranla aynı deterministik kuralı kullanır. */
    fun companionLineIndex(size: Int, seed: Int): Int = lineIndex(size, seed)
}

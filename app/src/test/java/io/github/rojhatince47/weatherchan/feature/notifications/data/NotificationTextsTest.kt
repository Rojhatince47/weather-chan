package io.github.rojhatince47.weatherchan.feature.notifications.data

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationPayload
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.SevereReason
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceCategory
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion.textArrayRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTextsTest {

    @Test
    fun `basliklar ture gore secilir`() {
        assertEquals(R.string.notif_title_severe, NotificationTexts.titleRes(NotificationType.SEVERE_WEATHER))
        assertEquals(R.string.notif_title_rain, NotificationTexts.titleRes(NotificationType.RAIN_SOON))
        assertEquals(R.string.notif_title_summary, NotificationTexts.titleRes(NotificationType.DAILY_SUMMARY))
        assertEquals(R.string.notif_title_companion, NotificationTexts.titleRes(NotificationType.COMPANION))
    }

    @Test
    fun `siddetli nedeni kendi metnine gider`() {
        val expected = mapOf(
            SevereReason.STORM to R.string.notif_severe_storm,
            SevereReason.SNOW to R.string.notif_severe_snow,
            SevereReason.WIND to R.string.notif_severe_wind,
            SevereReason.COLD to R.string.notif_severe_cold,
            SevereReason.HOT to R.string.notif_severe_hot
        )

        expected.forEach { (reason, res) ->
            assertEquals(
                res,
                NotificationTexts.textRes(NotificationPayload.Severe(reason))
            )
        }
    }

    @Test
    fun `yagmur ve ozet kendi metinlerine gider`() {
        assertEquals(
            R.string.notif_rain_soon,
            NotificationTexts.textRes(NotificationPayload.RainSoon)
        )
        assertEquals(
            R.string.notif_summary,
            NotificationTexts.textRes(
                NotificationPayload.Summary(
                    daily = DailyForecast(0, 5, 15, WeatherCondition.CLEAR),
                    rainChancePercent = 30
                )
            )
        )
    }

    @Test
    fun `karakter satiri tohuma gore deterministik`() {
        val first = NotificationTexts.companionLineIndex(size = 3, seed = 7)
        val second = NotificationTexts.companionLineIndex(size = 3, seed = 7)

        assertEquals(first, second)
        assertTrue(first in 0 until 3)
        assertTrue(NotificationTexts.companionLineIndex(size = 3, seed = 8) in 0 until 3)
    }

    @Test
    fun `karakter mesaji kategori havuzuna gider`() {
        AdviceCategory.entries.forEach { category ->
            assertEquals(
                category.textArrayRes(),
                NotificationTexts.textRes(NotificationPayload.Companion(category))
            )
        }
    }
}

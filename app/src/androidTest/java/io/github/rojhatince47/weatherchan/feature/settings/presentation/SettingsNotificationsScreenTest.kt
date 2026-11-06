package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsNotificationsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    private fun setNotificationsScreen(
        settings: NotificationSettings = NotificationSettings(),
        permissionGranted: Boolean = true,
        onMasterChange: (Boolean) -> Unit = {},
        onTypeChange: (NotificationType, Boolean) -> Unit = { _, _ -> },
        onSummaryTimeChange: (LocalTime) -> Unit = {},
        onOpenNotificationSettings: () -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        composeRule.setContent {
            HavaTheme {
                SettingsNotificationsScreen(
                    settings = settings,
                    permissionGranted = permissionGranted,
                    onMasterChange = onMasterChange,
                    onTypeChange = onTypeChange,
                    onSummaryTimeChange = onSummaryTimeChange,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onBack = onBack
                )
            }
        }
    }

    @Test
    fun izinKapaliykenUyariVeDugmeGorunur() {
        var opened = false
        setNotificationsScreen(
            permissionGranted = false,
            onOpenNotificationSettings = { opened = true }
        )

        composeRule.onNodeWithText("Bildirim izni kapalı").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_notif_permission").performClick()
        assertTrue(opened)
    }

    @Test
    fun izinAcikkenUyariGorunmez() {
        setNotificationsScreen(permissionGranted = true)

        composeRule.onNodeWithText("Bildirim izni kapalı").assertDoesNotExist()
    }

    @Test
    fun anaAnahtarCagrilir() {
        var enabled = true
        setNotificationsScreen(onMasterChange = { enabled = it })

        composeRule.onNodeWithTag("settings_notif_master").performClick()
        assertFalse(enabled)
    }

    @Test
    fun turAnahtariCagrilir() {
        var changed: Pair<NotificationType, Boolean>? = null
        setNotificationsScreen(onTypeChange = { type, enabled -> changed = type to enabled })

        composeRule.onNodeWithTag("settings_notif_rain_soon").performClick()
        assertEquals(NotificationType.RAIN_SOON to false, changed)
    }

    @Test
    fun anaAnahtarKapaliykenTurlerPasif() {
        setNotificationsScreen(settings = NotificationSettings(masterEnabled = false))

        composeRule.onNodeWithTag("settings_notif_rain_soon").assertIsNotEnabled()
    }

    @Test
    fun ozetSaatiGosterilir() {
        setNotificationsScreen(settings = NotificationSettings(summaryTime = LocalTime.of(9, 30)))

        composeRule.onNodeWithText("09:30").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun ozetSaatiDiyaloguAcilir() {
        setNotificationsScreen()

        composeRule.onNodeWithTag("settings_notif_summary_time").performScrollTo().performClick()
        composeRule.onNodeWithText("İptal").assertIsDisplayed()
    }

    @Test
    fun geriDugmesiGeriDonderir() {
        var back = false
        setNotificationsScreen(onBack = { back = true })

        composeRule.onNodeWithTag("action_back").performClick()
        assertTrue(back)
    }
}

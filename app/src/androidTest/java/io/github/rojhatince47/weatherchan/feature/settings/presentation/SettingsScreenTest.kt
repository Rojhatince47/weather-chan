package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    private fun setSettingsScreen(
        unit: TemperatureUnit = TemperatureUnit.CELSIUS,
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        appLanguage: AppLanguage = AppLanguage.TURKISH,
        soundEffectsEnabled: Boolean = true,
        notifications: NotificationSettings = NotificationSettings(),
        onUnitSelected: (TemperatureUnit) -> Unit = {},
        onSoundEffectsEnabledChange: (Boolean) -> Unit = {},
        onOpenCities: () -> Unit = {},
        onOpenNotifications: () -> Unit = {},
        onOpenAppearance: () -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        composeRule.setContent {
            HavaTheme {
                SettingsScreen(
                    unit = unit.toUiModel(),
                    themeMode = themeMode,
                    appLanguage = appLanguage,
                    soundEffectsEnabled = soundEffectsEnabled,
                    notificationSettings = notifications,
                    onUnitSelected = onUnitSelected,
                    onSoundEffectsEnabledChange = onSoundEffectsEnabledChange,
                    onOpenCities = onOpenCities,
                    onOpenNotifications = onOpenNotifications,
                    onOpenAppearance = onOpenAppearance,
                    onBack = onBack
                )
            }
        }
    }

    @Test
    fun kokBesSatiriGosterir() {
        setSettingsScreen()

        composeRule.onNodeWithText("Şehirler ve konum").assertIsDisplayed()
        composeRule.onNodeWithText("Birim").assertIsDisplayed()
        composeRule.onNodeWithText("Sesler").assertIsDisplayed()
        composeRule.onNodeWithText("Bildirimler").assertIsDisplayed()
        composeRule.onNodeWithText("Görünüm ve dil").assertIsDisplayed()
    }

    @Test
    fun birimSecimiCagrilir() {
        var selected: TemperatureUnit? = null
        setSettingsScreen(onUnitSelected = { selected = it })

        composeRule.onNodeWithText("°C").assertIsSelected()
        composeRule.onNodeWithText("°F").performClick()
        assertEquals(TemperatureUnit.FAHRENHEIT, selected)
    }

    @Test
    fun sesEfektleriAnahtariKapatilabilir() {
        var enabled = true
        setSettingsScreen(
            soundEffectsEnabled = enabled,
            onSoundEffectsEnabledChange = { enabled = it }
        )

        val switch = composeRule.onNodeWithTag("settings_sound_effects")
        switch.assertIsOn()
        switch.performClick()
        assertFalse(enabled)
    }

    @Test
    fun bildirimOzetiKapaliykenKapaliDer() {
        setSettingsScreen(
            notifications = NotificationSettings(masterEnabled = false)
        )

        composeRule.onNodeWithText("Kapalı").assertIsDisplayed()
    }

    @Test
    fun bildirimOzetiAcikkenSaatiGosterir() {
        setSettingsScreen(
            notifications = NotificationSettings(summaryTime = LocalTime.of(8, 0))
        )

        composeRule.onNodeWithText("Açık · 08:00").assertIsDisplayed()
    }

    @Test
    fun gorunumOzetiTemaVeDiliGosterir() {
        setSettingsScreen(
            themeMode = ThemeMode.DARK,
            appLanguage = AppLanguage.TURKISH
        )

        composeRule.onNodeWithText("Koyu · Türkçe").assertIsDisplayed()
    }

    @Test
    fun satirlarGezinmeCagrilariniTetikler() {
        var cities = false
        var notifications = false
        var appearance = false
        setSettingsScreen(
            onOpenCities = { cities = true },
            onOpenNotifications = { notifications = true },
            onOpenAppearance = { appearance = true }
        )

        composeRule.onNodeWithTag("settings_row_cities").performClick()
        composeRule.onNodeWithTag("settings_row_notifications").performClick()
        composeRule.onNodeWithTag("settings_row_appearance").performClick()

        assertTrue(cities)
        assertTrue(notifications)
        assertTrue(appearance)
    }

    @Test
    fun geriDugmesiGeriDonderir() {
        var back = false
        setSettingsScreen(onBack = { back = true })

        composeRule.onNodeWithTag("action_back").performClick()
        assertTrue(back)
    }
}

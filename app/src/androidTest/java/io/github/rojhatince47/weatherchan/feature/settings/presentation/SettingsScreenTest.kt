package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun birimSecimiCagrilir() {
        var selected: TemperatureUnit? = null
        composeRule.setContent {
            HavaTheme {
                SettingsScreen(
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM,
                    onUnitSelected = { selected = it },
                    onThemeModeSelected = {},
                    onBack = {}
                )
            }
        }
        composeRule.onNodeWithText("Birim").assertIsDisplayed()
        // Seçili durum eşlemesi: Santigrat veriliyken °C seçili görünür.
        composeRule.onNodeWithText("°C").assertIsSelected()
        composeRule.onNodeWithText("°F").performClick()
        assertEquals(TemperatureUnit.FAHRENHEIT, selected)
    }

    @Test
    fun temaSecimiCagrilir() {
        var selected: ThemeMode? = null
        composeRule.setContent {
            HavaTheme {
                SettingsScreen(
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM,
                    onUnitSelected = {},
                    onThemeModeSelected = { selected = it },
                    onBack = {}
                )
            }
        }
        composeRule.onNodeWithText("Tema").assertIsDisplayed()
        composeRule.onNodeWithText("Koyu").performClick()
        assertEquals(ThemeMode.DARK, selected)
    }

    @Test
    fun geriDugmesiGeriDonderir() {
        var back = false
        composeRule.setContent {
            HavaTheme {
                SettingsScreen(
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM,
                    onUnitSelected = {},
                    onThemeModeSelected = {},
                    onBack = { back = true }
                )
            }
        }
        composeRule.onNodeWithTag("action_back").performClick()
        assertTrue(back)
    }
}

package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsAppearanceScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    private fun setAppearanceScreen(
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        appLanguage: AppLanguage = AppLanguage.TURKISH,
        onThemeModeSelected: (ThemeMode) -> Unit = {},
        onAppLanguageSelected: (AppLanguage) -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        composeRule.setContent {
            HavaTheme {
                SettingsAppearanceScreen(
                    themeMode = themeMode,
                    appLanguage = appLanguage,
                    onThemeModeSelected = onThemeModeSelected,
                    onAppLanguageSelected = onAppLanguageSelected,
                    onBack = onBack
                )
            }
        }
    }

    @Test
    fun temaSecimiCagrilir() {
        var selected: ThemeMode? = null
        setAppearanceScreen(onThemeModeSelected = { selected = it })

        composeRule.onNodeWithText("Koyu").performClick()
        assertEquals(ThemeMode.DARK, selected)
    }

    @Test
    fun dilSecimiCagrilir() {
        var selected: AppLanguage? = null
        setAppearanceScreen(onAppLanguageSelected = { selected = it })

        composeRule.onNodeWithText("English").performClick()
        assertEquals(AppLanguage.ENGLISH, selected)
    }

    @Test
    fun dilIngilizceykenBasliklarIngilizceDoner() {
        // Host etkinliği Türkçe açılır; İngilizce, etkinlik yeniden
        // kurulduğunda kaynaklardan çözümlenir.
        HavaTestActivity.overrideLocaleTag = "en"
        try {
            composeRule.activityRule.scenario.recreate()
            setAppearanceScreen(appLanguage = AppLanguage.ENGLISH)
            composeRule.onNodeWithText("Theme").assertIsDisplayed()
            composeRule.onNodeWithText("Language").assertIsDisplayed()
        } finally {
            HavaTestActivity.overrideLocaleTag = null
        }
    }

    @Test
    fun geriDugmesiGeriDonderir() {
        var back = false
        setAppearanceScreen(onBack = { back = true })

        composeRule.onNodeWithTag("action_back").performClick()
        assertTrue(back)
    }
}

package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    /** Konum bölümü için tek biçim yardımcı: `locationState` provider'dır. */
    private fun setSettingsScreen(
        locationState: () -> LocationState = { LocationState.Idle },
        locationAdded: Boolean = false,
        onAddLocation: () -> Unit = {},
        onRetryLocation: () -> Unit = {}
    ) {
        composeRule.setContent {
            HavaTheme {
                SettingsScreen(
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM,
                    appLanguage = AppLanguage.SYSTEM,
                    locationState = locationState(),
                    locationAdded = locationAdded,
                    onAddLocation = onAddLocation,
                    onRetryLocation = onRetryLocation,
                    onUnitSelected = {},
                    onThemeModeSelected = {},
                    onAppLanguageSelected = {},
                    onBack = {}
                )
            }
        }
    }

    @Test
    fun konumBolumuIdledeGorunurVeEkler() {
        var clicked = false
        setSettingsScreen(locationState = { LocationState.Idle }, onAddLocation = { clicked = true })
        composeRule.onNodeWithText("Konum").assertIsDisplayed()
        composeRule.onNodeWithText("Konumumu ekle").performClick()
        assertTrue(clicked)
    }

    @Test
    fun konumBolumuUnavailabledaTekrarDeneGosterir() {
        var retried = false
        setSettingsScreen(locationState = { LocationState.Unavailable }, onRetryLocation = { retried = true })
        composeRule.onNodeWithTag("add_location_row").assertIsDisplayed()
        composeRule.onNodeWithText("Tekrar dene").performClick()
        assertTrue(retried)
    }

    @Test
    fun konumBolumuLoadingdeGostergeGosterir() {
        setSettingsScreen(locationState = { LocationState.Loading })
        composeRule.onNodeWithTag("location_loading").assertIsDisplayed()
        composeRule.onNodeWithTag("add_location_row").assertDoesNotExist()
    }

    @Test
    fun konumBolumuKatEklendigindeGizlenir() {
        setSettingsScreen(locationAdded = true)
        composeRule.onNodeWithText("Konum").assertDoesNotExist()
        composeRule.onNodeWithText("Konumumu ekle").assertDoesNotExist()
    }

    @Test
    fun konumBolumuResolvedOluncaGizlenir() {
        setSettingsScreen(locationState = { LocationState.Resolved })
        composeRule.onNodeWithText("Konum").assertDoesNotExist()
        composeRule.onNodeWithText("Konumumu ekle").assertDoesNotExist()
    }

    @Test
    fun konumBolumuCanliOlarakGizlenir() {
        var locationState by mutableStateOf<LocationState>(LocationState.Idle)
        setSettingsScreen(locationState = { locationState })
        composeRule.onNodeWithText("Konumumu ekle").assertIsDisplayed()
        locationState = LocationState.Resolved
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Konumumu ekle").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun birimSecimiCagrilir() {
        var selected: TemperatureUnit? = null
        composeRule.setContent {
            HavaTheme {
                SettingsScreen(
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM,
                    appLanguage = AppLanguage.SYSTEM,
                    onAppLanguageSelected = {},
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
                    appLanguage = AppLanguage.SYSTEM,
                    onUnitSelected = {},
                    onAppLanguageSelected = {},
                    onThemeModeSelected = { selected = it },
                    onBack = {}
                )
            }
        }
        composeRule.onNodeWithText("Tema").assertIsDisplayed()
        composeRule.onNodeWithText("Koyu").performScrollTo().performClick()
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
                    appLanguage = AppLanguage.SYSTEM,
                    onUnitSelected = {},
                    onAppLanguageSelected = {},
                    onThemeModeSelected = {},
                    onBack = { back = true }
                )
            }
        }
        composeRule.onNodeWithTag("action_back").performClick()
        assertTrue(back)
    }

    @Test
    fun languageSelectionInvokesCallback() {
        var selected: AppLanguage? = null
        composeRule.setContent {
            HavaTheme {
                SettingsScreen(
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM,
                    appLanguage = AppLanguage.SYSTEM,
                    onUnitSelected = {},
                    onThemeModeSelected = {},
                    onAppLanguageSelected = { selected = it },
                    onBack = {}
                )
            }
        }
        composeRule.onNodeWithText("English").performScrollTo().performClick()
        assertEquals(AppLanguage.ENGLISH, selected)
    }

    @Test
    fun dilIngilizceyeGecinceMetinlerIngilizceDoner() {
        // Host etkinliği Türkçe açılır; İngilizce, etkinlik yeniden
        // kurulduğunda kaynaklardan çözümlenir.
        HavaTestActivity.overrideLocaleTag = "en"
        try {
            composeRule.activityRule.scenario.recreate()
            composeRule.setContent {
                HavaTheme {
                    SettingsScreen(
                        unit = TemperatureUnit.CELSIUS.toUiModel(),
                        themeMode = ThemeMode.SYSTEM,
                        appLanguage = AppLanguage.ENGLISH,
                        onUnitSelected = {},
                        onThemeModeSelected = {},
                        onAppLanguageSelected = {},
                        onBack = {}
                    )
                }
            }
            composeRule.onNodeWithText("Language").assertIsDisplayed()
            composeRule.onNodeWithText("Theme").assertIsDisplayed()
        } finally {
            HavaTestActivity.overrideLocaleTag = null
        }
    }
}

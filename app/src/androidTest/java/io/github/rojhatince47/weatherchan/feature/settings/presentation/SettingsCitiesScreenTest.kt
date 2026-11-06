package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsCitiesScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    private fun setCitiesScreen(
        locationState: () -> LocationState = { LocationState.Idle },
        locationAdded: Boolean = false,
        onAddLocation: () -> Unit = {},
        onRetryLocation: () -> Unit = {},
        onOpenCityList: () -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        composeRule.setContent {
            HavaTheme {
                SettingsCitiesScreen(
                    locationState = locationState(),
                    locationAdded = locationAdded,
                    onAddLocation = onAddLocation,
                    onRetryLocation = onRetryLocation,
                    onOpenCityList = onOpenCityList,
                    onBack = onBack
                )
            }
        }
    }

    @Test
    fun konumBolumuIdledeGorunurVeEkler() {
        var clicked = false
        setCitiesScreen(locationState = { LocationState.Idle }, onAddLocation = { clicked = true })

        composeRule.onNodeWithText("Konum").assertIsDisplayed()
        composeRule.onNodeWithText("Konumumu ekle").performClick()
        assertTrue(clicked)
    }

    @Test
    fun konumBolumuUnavailabledaTekrarDeneGosterir() {
        var retried = false
        setCitiesScreen(locationState = { LocationState.Unavailable }, onRetryLocation = { retried = true })

        composeRule.onNodeWithTag("add_location_row").assertIsDisplayed()
        composeRule.onNodeWithText("Tekrar dene").performClick()
        assertTrue(retried)
    }

    @Test
    fun konumBolumuLoadingdeGostergeGosterir() {
        setCitiesScreen(locationState = { LocationState.Loading })
        composeRule.onNodeWithTag("location_loading").assertIsDisplayed()
        composeRule.onNodeWithTag("add_location_row").assertDoesNotExist()
    }

    @Test
    fun konumBolumuKatEklendigindeGizlenir() {
        setCitiesScreen(locationAdded = true)
        composeRule.onNodeWithText("Konum").assertDoesNotExist()
        composeRule.onNodeWithText("Konumumu ekle").assertDoesNotExist()
    }

    @Test
    fun konumBolumuResolvedOluncaGizlenir() {
        setCitiesScreen(locationState = { LocationState.Resolved })
        composeRule.onNodeWithText("Konum").assertDoesNotExist()
        composeRule.onNodeWithText("Konumumu ekle").assertDoesNotExist()
    }

    @Test
    fun konumBolumuCanliOlarakGizlenir() {
        var locationState by mutableStateOf<LocationState>(LocationState.Idle)
        setCitiesScreen(locationState = { locationState })
        composeRule.onNodeWithText("Konumumu ekle").assertIsDisplayed()
        locationState = LocationState.Resolved
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Konumumu ekle").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun sehirlerimSatiriListeyiAcar() {
        var opened = false
        setCitiesScreen(onOpenCityList = { opened = true })

        composeRule.onNodeWithTag("settings_row_city_list").performClick()
        assertTrue(opened)
    }

    @Test
    fun geriDugmesiGeriDonderir() {
        var back = false
        setCitiesScreen(onBack = { back = true })

        composeRule.onNodeWithTag("action_back").performClick()
        assertTrue(back)
    }
}

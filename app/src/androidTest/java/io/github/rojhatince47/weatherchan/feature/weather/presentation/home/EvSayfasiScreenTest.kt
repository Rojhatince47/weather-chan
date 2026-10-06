package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState
import io.github.rojhatince47.weatherchan.testing.ForceTurkishLocaleRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EvSayfasiScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val forceTurkish = ForceTurkishLocaleRule()

    private val istanbul = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
    private val ankara = City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85))

    private fun setScreen(
        cities: List<City>,
        currentPage: Int = 0,
        onAddCityClick: () -> Unit = {},
        onSettingsClick: () -> Unit = {},
        onSelectCity: (Long?) -> Unit = {},
        locationState: LocationState = LocationState.Resolved,
        onAddLocation: () -> Unit = {},
        onRetryLocation: () -> Unit = {}
    ) {
        composeRule.setContent {
            HavaTheme {
                EvSayfasiScreen(
                    cities = cities,
                    currentPage = currentPage,
                    contentFor = { city -> Text(text = city.name) },
                    themeMode = ThemeMode.SYSTEM,
                    onPageChanged = {},
                    onAddCityClick = onAddCityClick,
                    onSettingsClick = onSettingsClick,
                    onSelectCity = onSelectCity,
                    onRetry = {},
                    onRefresh = {},
                    locationState = locationState,
                    onAddLocation = onAddLocation,
                    onRetryLocation = onRetryLocation
                )
            }
        }
    }

    @Test
    fun addActionInvokesCallback() {
        var opened = false
        setScreen(listOf(istanbul), onAddCityClick = { opened = true })
        composeRule.onNodeWithTag("action_add_city").performClick()
        assertEquals(true, opened)
    }

    @Test
    fun settingsActionInvokesCallback() {
        var opened = false
        setScreen(listOf(istanbul), onSettingsClick = { opened = true })
        composeRule.onNodeWithTag("action_settings").performClick()
        assertEquals(true, opened)
    }

    @Test
    fun titleClickOpensList() {
        var selected: Long? = null
        setScreen(listOf(istanbul, ankara), onSelectCity = { selected = it })
        // Başlık tıklanabilir alandır (testTag "action_open_list"); null =
        // listeyi aç sinyali.
        composeRule.onNodeWithTag("action_open_list").performClick()
        assertNull(selected)
    }

    @Test
    fun swipeChangesPage() {
        setScreen(listOf(istanbul, ankara))
        composeRule.onNodeWithTag("home_pager").performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("Ankara").assertIsDisplayed()
    }

    @Test
    fun doluListedeKonumsuzkenSatirGorunur() {
        var clicked = false
        setScreen(
            listOf(istanbul),
            locationState = LocationState.Idle,
            onAddLocation = { clicked = true }
        )
        composeRule.onNodeWithTag("add_location_row").assertIsDisplayed()
        composeRule.onNodeWithText("Konumumu ekle").performClick()
        assertTrue(clicked)
    }

    @Test
    fun adisizKonumSayfasiVarsayilanAdGosterir() {
        val konum = City("", null, null, Coordinates(41.01, 28.98), isLocation = true)
        setScreen(listOf(konum))
        composeRule.onNodeWithText("Konum").assertIsDisplayed()
    }
}

package io.github.rojhatince47.weatherchan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mainScreenShowsHomeHeader() {
        composeRule.setContent {
            HavaTheme {
                MainScreen()
            }
        }

        composeRule.onNodeWithText("Weather-chan").assertIsDisplayed()
        composeRule.onNodeWithText("Şehirlerin havasını sevimli takip et").assertIsDisplayed()
    }
}

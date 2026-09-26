package io.github.rojhatince47.weatherchan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.rojhatince47.weatherchan.core.ui.component.HomeHeader
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import org.junit.Rule
import org.junit.Test

class HomeHeaderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeHeaderShowsProvidedTitleAndDescription() {
        composeRule.setContent {
            HavaTheme {
                HomeHeader(
                    title = "Test Title",
                    description = "Test Description"
                )
            }
        }

        composeRule.onNodeWithText("Test Title").assertIsDisplayed()
        composeRule.onNodeWithText("Test Description").assertIsDisplayed()
    }
}

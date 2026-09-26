package io.github.rojhatince47.weatherchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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

    @Test
    fun homeHeaderRemainsReadableWithNarrowScreenAndLargeFontScale() {
        val longTitle = "Çok uzun bir başlık örneği için preview"
        val longDescription = "Bu açıklama metni, uzun metinlerin bile okunabilir ve " +
            "düzenli kalmasını sağlamak için kullanılır."

        composeRule.setContent {
            HavaTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(density = 1f, fontScale = 1.5f)
                ) {
                    Box(modifier = Modifier.width(320.dp)) {
                        HomeHeader(
                            title = longTitle,
                            description = longDescription
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText(longTitle).assertIsDisplayed()
        composeRule.onNodeWithText(longDescription).assertIsDisplayed()
    }
}

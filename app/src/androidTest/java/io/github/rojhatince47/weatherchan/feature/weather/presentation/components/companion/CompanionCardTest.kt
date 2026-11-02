package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionPalette
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceCategory
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CompanionUiModel
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CompanionCardTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    private fun render(animationsEnabled: Boolean) {
        composeRule.setContent {
            HavaTheme {
                CompanionCard(
                    advice = CompanionUiModel(
                        pose = CompanionPose.CHEERFUL,
                        category = AdviceCategory.CLEAR,
                        variantSeed = 0
                    ),
                    colors = ConditionPalette.of(WeatherCondition.CLEAR, isDark = false, isDay = true),
                    animationsEnabled = animationsEnabled
                )
            }
        }
    }

    private fun bubbleText(): String = composeRule
        .onNodeWithTag("companion_bubble", useUnmergedTree = true)
        .fetchSemanticsNode()
        .config[SemanticsProperties.Text]
        .joinToString("") { it.text }

    @Test
    fun kartGorunurVeBalonHavaRepliginiTasir() {
        render(animationsEnabled = false)

        composeRule.onNodeWithTag("companion_card").assertIsDisplayed()
        val lines = composeRule.activity.resources.getStringArray(R.array.advice_clear)
        assertTrue(bubbleText() in lines)
    }

    @Test
    fun karakterGorseliIcerikAciklamasiTasir() {
        render(animationsEnabled = false)

        composeRule
            .onNodeWithTag("companion_art", useUnmergedTree = true)
            .assertContentDescriptionEquals("Weather-chan, neşeli pozunda")
        composeRule
            .onNodeWithTag("companion_card")
            .assertContentDescriptionEquals("Weather-chan, neşeli pozunda")
    }

    @Test
    fun kartEnAzKirkSekizDpDokunmaHedefidir() {
        render(animationsEnabled = false)

        val bounds = composeRule.onNodeWithTag("companion_card").getUnclippedBoundsInRoot()
        assertTrue("genislik", (bounds.right - bounds.left) >= 48.dp)
        assertTrue("yukseklik", (bounds.bottom - bounds.top) >= 48.dp)
    }

    @Test
    fun kartEtiketiVardir() {
        render(animationsEnabled = false)

        composeRule.onNodeWithTag("companion_card").assertExists()
    }
}

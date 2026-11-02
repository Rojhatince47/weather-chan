package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionPalette
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceCategory
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CompanionUiModel
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import org.junit.Assert.assertEquals
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

    @Test
    fun sanatPanosuYeniBoyuttaCizilir() {
        render(animationsEnabled = false)

        val bounds = composeRule.onNodeWithTag("companion_art", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(128f, (bounds.right - bounds.left).value, 1f)
        assertEquals(160f, (bounds.bottom - bounds.top).value, 1f)
    }

    @Test
    fun dokunusTapRepligineVeSurprisedPozaGecirir() {
        render(animationsEnabled = false)
        composeRule.onNodeWithTag("companion_card").performClick()

        val tapLines = composeRule.activity.resources.getStringArray(R.array.companion_tap_reactions)
        assertTrue(bubbleText() in tapLines)
        composeRule.onNodeWithContentDescription(
            "şaşkın",
            substring = true,
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun tepkiBittiktenSonraHavaRepligineDoner() {
        render(animationsEnabled = false)
        composeRule.onNodeWithTag("companion_card").performClick()

        val tapLines = composeRule.activity.resources.getStringArray(R.array.companion_tap_reactions)
        assertTrue("once tap repligi gelmeli", bubbleText() in tapLines)
        val adviceLines = composeRule.activity.resources.getStringArray(R.array.advice_clear)
        composeRule.waitUntil(5_000) { bubbleText() in adviceLines }
    }

    @Test
    fun dokunusSemantigiYalnizBalonMetniVeSanatAciklamasiTasir() {
        render(animationsEnabled = false)
        composeRule.onNodeWithTag("companion_card").performClick()

        val node = composeRule.onNodeWithTag("companion_card").fetchSemanticsNode()
        val texts = node.config[SemanticsProperties.Text]
        assertEquals("tepki sembolleri metin dugumu eklememeli", 1, texts.size)
        assertTrue(
            "balon tap repligine donmedi",
            texts.single().text in composeRule.activity.resources.getStringArray(R.array.companion_tap_reactions)
        )
        val descriptions = node.config[SemanticsProperties.ContentDescription]
        assertEquals("sanat aciklamasi tek olmali", 1, descriptions.size)
        assertTrue("efektif poz aciklamaya yansimadi", descriptions.single().contains("şaşkın"))
    }

    @Test
    fun ltrYonundeGorselSoldaBalonSagda() {
        render(animationsEnabled = false)
        val art = composeRule.onNodeWithTag("companion_art", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val bubble = composeRule.onNodeWithTag("companion_bubble", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(art.right <= bubble.left)
    }

    @Test
    fun rtlYonundeGorselSagdaBalonSolda() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HavaTheme {
                    CompanionCard(
                        advice = CompanionUiModel(
                            pose = CompanionPose.CHEERFUL,
                            category = AdviceCategory.CLEAR,
                            variantSeed = 0
                        ),
                        colors = ConditionPalette.of(WeatherCondition.CLEAR, isDark = false, isDay = true),
                        animationsEnabled = false
                    )
                }
            }
        }
        val art = composeRule.onNodeWithTag("companion_art", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val bubble = composeRule.onNodeWithTag("companion_bubble", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(art.left >= bubble.right)
    }

    @Test
    fun buyukYaziTipindeVeDarGenislikteBalonTasmaz() {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                HavaTheme {
                    Box(Modifier.width(280.dp)) {
                        CompanionCard(
                            advice = CompanionUiModel(
                                pose = CompanionPose.CHEERFUL,
                                category = AdviceCategory.CLEAR,
                                variantSeed = 0
                            ),
                            colors = ConditionPalette.of(WeatherCondition.CLEAR, isDark = false, isDay = true),
                            animationsEnabled = false
                        )
                    }
                }
            }
        }

        val card = composeRule.onNodeWithTag("companion_card").getUnclippedBoundsInRoot()
        val bubble = composeRule.onNodeWithTag("companion_bubble", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("kart sabit panodan buyumeli", (card.bottom - card.top) > 140.dp)
        assertTrue("balon kartin ust sinirini asiyor", bubble.top >= card.top)
        assertTrue("balon kartin alt sinirini asiyor", bubble.bottom <= card.bottom)
        assertTrue("balon karti yatay asiyor", bubble.left >= card.left && bubble.right <= card.right)
        assertTrue(bubbleText().isNotBlank())
    }

    private class RecordingAudio : CompanionAudioPlayer {
        var taps = 0

        override fun playTap() {
            taps++
        }

        override fun release() = Unit
    }

    @Test
    fun dokunusSesOynaticisinaIletilir() {
        val audio = RecordingAudio()
        composeRule.setContent {
            HavaTheme {
                CompanionCard(
                    advice = CompanionUiModel(
                        pose = CompanionPose.CHEERFUL,
                        category = AdviceCategory.CLEAR,
                        variantSeed = 0
                    ),
                    colors = ConditionPalette.of(WeatherCondition.CLEAR, isDark = false, isDay = true),
                    audio = audio,
                    animationsEnabled = false
                )
            }
        }
        composeRule.onNodeWithTag("companion_card").performClick()
        composeRule.runOnIdle { assertEquals(1, audio.taps) }
    }

    @Test
    fun yerTutucuSesDosyalariOkunabilir() {
        CompanionSoundAssets.tapClips.forEach { id ->
            composeRule.activity.resources.openRawResource(id).use { assertTrue(it.available() > 0) }
        }
    }
}

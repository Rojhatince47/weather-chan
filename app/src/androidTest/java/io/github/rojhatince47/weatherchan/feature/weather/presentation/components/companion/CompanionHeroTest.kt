package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionPalette
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceCategory
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CompanionUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CurrentWeatherUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherIcon
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CompanionHeroTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    private fun weather() = CurrentWeatherUiModel(
        temperatureText = "18°C",
        icon = WeatherIcon.Glyph("☀"),
        conditionLabel = UiText.Resource(R.string.condition_clear),
        windSpeedText = "8 km/s",
        humidityText = "%50",
        condition = WeatherCondition.CLEAR,
        isDay = true
    )

    private fun advice() = CompanionUiModel(
        pose = CompanionPose.CHEERFUL,
        category = AdviceCategory.CLEAR,
        variantSeed = 0
    )

    private fun render(animationsEnabled: Boolean = false, direction: LayoutDirection = LayoutDirection.Ltr) {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                HavaTheme {
                    CompanionHero(
                        advice = advice(),
                        currentWeather = weather(),
                        colors = ConditionPalette.of(WeatherCondition.CLEAR, isDark = false, isDay = true),
                        animationsEnabled = animationsEnabled
                    )
                }
            }
        }
    }

    private fun bubbleText(): String = composeRule
        .onNodeWithTag("companion_bubble")
        .fetchSemanticsNode()
        .config[SemanticsProperties.Text]
        .joinToString("") { it.text }

    private fun bounds(tag: String) = composeRule
        .onNodeWithTag(tag, useUnmergedTree = true)
        .getUnclippedBoundsInRoot()

    @Test
    fun heroHavaVeTavsiyeyiBirlikteGosterir() {
        render()

        composeRule.onNodeWithTag("companion_hero").assertIsDisplayed()
        composeRule.onNodeWithTag("companion_weather").assertExists()
        composeRule.onNodeWithText("18°C").assertExists()
        composeRule.onNodeWithText("Açık").assertExists()
        assertTrue(bubbleText() in composeRule.activity.resources.getStringArray(R.array.advice_clear))
    }

    @Test
    fun sanatPanosuBoyutu() {
        render()
        val art = bounds("companion_art")
        assertEquals(148f, (art.right - art.left).value, 1f)
        assertEquals(186f, (art.bottom - art.top).value, 1f)
    }

    @Test
    fun balonKuyruguKaraktereBakar() {
        render()
        val tail = bounds("companion_tail")
        val bubble = bounds("companion_bubble")
        assertTrue("kuyruk balonun ustunde", tail.top < bubble.top)
        assertTrue("kuyruk balona degmeli", tail.bottom <= bubble.top + 1.dp)
        assertTrue("kuyruk gorunur genislikte", (tail.right - tail.left) >= 12.dp)
    }

    @Test
    fun kartDokunmaHedefiEnAzKirkSekizDp() {
        render()
        val hero = bounds("companion_hero")
        assertTrue((hero.right - hero.left) >= 48.dp)
        assertTrue((hero.bottom - hero.top) >= 48.dp)
    }

    @Test
    fun karakterGorseliIcerikAciklamasiTasir() {
        render()

        composeRule
            .onNodeWithTag("companion_art")
            .assertContentDescriptionEquals("Weather-chan, neşeli pozunda")
    }

    @Test
    fun havaSutunuTiklanabilirDegil() {
        render()
        composeRule.onNodeWithTag("companion_weather").assertHasNoClickAction()
    }

    @Test
    fun karaktereDokunusTapRepligineGecirir() {
        render()
        composeRule.onNodeWithTag("companion_art").performClick()

        val tapLines = composeRule.activity.resources.getStringArray(R.array.companion_tap_reactions)
        assertTrue(bubbleText() in tapLines)
        composeRule.onNodeWithContentDescription(
            "şaşkın",
            substring = true,
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun balonaDokunusTapRepligineGecirir() {
        render()
        composeRule.onNodeWithTag("companion_bubble").performClick()

        assertTrue(
            bubbleText() in composeRule.activity.resources.getStringArray(R.array.companion_tap_reactions)
        )
    }

    @Test
    fun tepkiBittiktenSonraHavaRepligineDoner() {
        render()
        composeRule.onNodeWithTag("companion_art").performClick()

        val tapLines = composeRule.activity.resources.getStringArray(R.array.companion_tap_reactions)
        assertTrue("once tap repligi gelmeli", bubbleText() in tapLines)
        val adviceLines = composeRule.activity.resources.getStringArray(R.array.advice_clear)
        composeRule.waitUntil(5_000) { bubbleText() in adviceLines }
    }

    @Test
    fun ltrYonundeKarakterSoldaHavaSagda() {
        render(direction = LayoutDirection.Ltr)
        assertTrue(bounds("companion_art").right <= bounds("companion_weather").left)
    }

    @Test
    fun rtlYonundeKarakterSagdaHavaSolda() {
        render(direction = LayoutDirection.Rtl)
        assertTrue(bounds("companion_art").left >= bounds("companion_weather").right)
    }

    @Test
    fun buyukYaziTipindeVeDarGenislikteTasmaYok() {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                HavaTheme {
                    Box(Modifier.width(280.dp)) {
                        CompanionHero(
                            advice = advice(),
                            currentWeather = weather(),
                            colors = ConditionPalette.of(WeatherCondition.CLEAR, isDark = false, isDay = true),
                            animationsEnabled = false
                        )
                    }
                }
            }
        }

        val hero = bounds("companion_hero")
        val weather = bounds("companion_weather")
        val bubble = bounds("companion_bubble")
        assertTrue("hero buyumeli", (hero.bottom - hero.top) > 170.dp)
        assertTrue("hava hero icinde", weather.right <= hero.right)
        assertTrue("balon hero icinde", bubble.left >= hero.left && bubble.right <= hero.right)
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
                CompanionHero(
                    advice = advice(),
                    currentWeather = weather(),
                    colors = ConditionPalette.of(WeatherCondition.CLEAR, isDark = false, isDay = true),
                    audio = audio,
                    animationsEnabled = false
                )
            }
        }
        composeRule.onNodeWithTag("companion_art").performClick()
        composeRule.runOnIdle { assertEquals(1, audio.taps) }
    }

    @Test
    fun yerTutucuSesDosyalariOkunabilir() {
        CompanionSoundAssets.tapClips.forEach { id ->
            composeRule.activity.resources.openRawResource(id).use { assertTrue(it.available() > 0) }
        }
    }
}

package io.github.rojhatince47.weatherchan.feature.weather.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.github.rojhatince47.weatherchan.core.ui.theme.WeatherBackgroundPalette
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class WeatherBackgroundTest {

    @get:Rule
    val rule = createComposeRule()

    // Tek setContent kuralı: sahne tek kez kurulur, palet durumla değiştirilir.
    private var palette by mutableStateOf<WeatherBackgroundPalette?>(null)
    private var animatorsOn by mutableStateOf(false)
    private var mounted = false

    private fun mount() {
        rule.setContent {
            palette?.let { p ->
                Box(Modifier.fillMaxSize().testTag("bg")) {
                    WeatherBackground(palette = p, animatorsEnabled = animatorsOn)
                }
            }
        }
    }

    private fun render(target: WeatherBackgroundPalette, animatorsEnabled: Boolean = false) {
        if (!mounted) {
            animatorsOn = animatorsEnabled
            palette = target
            mount()
            mounted = true
        } else {
            rule.runOnIdle {
                animatorsOn = animatorsEnabled
                palette = target
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun gradientChangesVertically() {
        render(WeatherBackgroundPalette.of(WeatherCondition.CLEAR, isDark = false, isDay = true))
        val image = rule.onNodeWithTag("bg").captureToImage().asAndroidBitmap()
        val top = image.getPixel(image.width / 2, 8)
        val bottom = image.getPixel(image.width / 2, image.height - 8)
        assertNotEquals("gradyan duz", top, bottom)
    }

    @Test
    fun dayAndNightScenesDiffer() {
        render(WeatherBackgroundPalette.of(WeatherCondition.CLEAR, isDark = false, isDay = true))
        val dayImage = rule.onNodeWithTag("bg").captureToImage().asAndroidBitmap()
        val dayTop = dayImage.getPixel(dayImage.width / 2, 8)
        render(WeatherBackgroundPalette.of(WeatherCondition.CLEAR, isDark = false, isDay = false))
        val nightImage = rule.onNodeWithTag("bg").captureToImage().asAndroidBitmap()
        assertNotEquals("gunduz/gece ust piksel ayni", dayTop, nightImage.getPixel(nightImage.width / 2, 8))
    }

    @Test
    fun everyConditionRenders() {
        WeatherCondition.entries.forEach { condition ->
            listOf(false, true).forEach { isDark ->
                listOf(true, false).forEach { isDay ->
                    render(WeatherBackgroundPalette.of(condition, isDark = isDark, isDay = isDay))
                    rule.onNodeWithTag("bg").captureToImage()
                }
            }
        }
    }

    @Test
    fun staticSceneDrawsDecor() {
        render(WeatherBackgroundPalette.of(WeatherCondition.CLEAR, isDark = false, isDay = true))
        val image = rule.onNodeWithTag("bg").captureToImage().asAndroidBitmap()
        // Güneş diski (~0.78, ~0.18) ile altındaki gökyüzü pikseli farklı olmalı.
        val sun = image.getPixel((image.width * 0.78f).toInt(), (image.height * 0.18f).toInt())
        val sky = image.getPixel((image.width * 0.78f).toInt(), (image.height * 0.55f).toInt())
        assertNotEquals("gok cismi cizilmemis", sun, sky)
    }

    @Test
    fun animatorsDisabledKeepsSceneStatic() {
        val palette = WeatherBackgroundPalette.of(WeatherCondition.SNOW, isDark = true, isDay = false)
        render(palette, animatorsEnabled = false)
        rule.onNodeWithTag("bg").captureToImage()
    }
}

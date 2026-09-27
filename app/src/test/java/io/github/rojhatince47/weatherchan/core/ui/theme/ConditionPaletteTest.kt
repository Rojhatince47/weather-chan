package io.github.rojhatince47.weatherchan.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionPaletteTest {

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun `every condition has light colors`() {
        WeatherCondition.entries.forEach { condition ->
            val colors = ConditionPalette.of(condition, isDark = false)
            assertTrue("$condition light container yok", colors.container.value != 0UL)
            assertTrue("$condition light accent yok", colors.accent.value != 0UL)
        }
    }

    @Test
    fun `every condition has dark colors`() {
        WeatherCondition.entries.forEach { condition ->
            val colors = ConditionPalette.of(condition, isDark = true)
            assertTrue("$condition dark container yok", colors.container.value != 0UL)
            assertTrue("$condition dark accent yok", colors.accent.value != 0UL)
        }
    }

    @Test
    fun `light palette meets wcag aa for on container text`() {
        WeatherCondition.entries.forEach { condition ->
            val colors = ConditionPalette.of(condition, isDark = false)
            val ratio = contrastRatio(colors.onContainer, colors.container)
            assertTrue(
                "$condition light kontrastı yetersiz: $ratio",
                ratio >= 4.5
            )
        }
    }

    @Test
    fun `dark palette meets wcag aa for on container text`() {
        WeatherCondition.entries.forEach { condition ->
            val colors = ConditionPalette.of(condition, isDark = true)
            val ratio = contrastRatio(colors.onContainer, colors.container)
            assertTrue(
                "$condition dark kontrastı yetersiz: $ratio",
                ratio >= 4.5
            )
        }
    }

    @Test
    fun `light and dark differ for every condition`() {
        WeatherCondition.entries.forEach { condition ->
            assertTrue(
                "$condition light ve dark paleti aynı",
                ConditionPalette.of(condition, isDark = false).container !=
                    ConditionPalette.of(condition, isDark = true).container
            )
        }
    }

    @Test
    fun `weather condition has a palette entry`() {
        assertEquals(6, ConditionPalette.colorsFor(isDark = false).size)
    }
}

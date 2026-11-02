package io.github.rojhatince47.weatherchan.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
    fun `gece container gunduzden koyudur`() {
        WeatherCondition.entries.forEach { condition ->
            listOf(false, true).forEach { isDark ->
                val day = ConditionPalette.of(condition, isDark = isDark, isDay = true)
                val night = ConditionPalette.of(condition, isDark = isDark, isDay = false)
                assertTrue(
                    "$condition gece container gündüzden koyu olmalı",
                    night.container.luminance() < day.container.luminance()
                )
                assertTrue(
                    "$condition gece/gündüz container farklı olmalı",
                    night.container != day.container
                )
            }
        }
    }

    @Test
    fun `gece paleti metin kontrastini korur`() {
        WeatherCondition.entries.forEach { condition ->
            listOf(false, true).forEach { isDark ->
                val night = ConditionPalette.of(condition, isDark = isDark, isDay = false)
                val ratio = contrastRatio(night.onContainer, night.container)
                assertTrue(
                    "$condition gece kontrastı yetersiz: $ratio",
                    ratio >= 4.5
                )
            }
        }
    }

    @Test
    fun `weather condition has a palette entry`() {
        assertEquals(6, ConditionPalette.colorsFor(isDark = false).size)
    }

    @Test
    fun `hafif saganak yagmur paletini paylasir`() {
        // Ayrı renk yok (bilinçli): yumuşaklık simge + etikettedir; kart
        // yağmur ailesinde kalır. Normalleştirme kırılırsa bu test düşer.
        listOf(false, true).forEach { isDark ->
            listOf(true, false).forEach { isDay ->
                assertEquals(
                    ConditionPalette.of(WeatherCondition.RAIN, isDark = isDark, isDay = isDay),
                    ConditionPalette.of(WeatherCondition.LIGHT_SHOWERS, isDark = isDark, isDay = isDay)
                )
            }
        }
    }

    @Test
    fun `balon zemini tum kosullarda aa korur`() {
        WeatherCondition.entries.forEach { condition ->
            listOf(false, true).forEach { isDark ->
                listOf(false, true).forEach { isDay ->
                    val colors = ConditionPalette.of(condition, isDark, isDay)
                    val bubble = lerp(colors.container, colors.accent, 0.08f)
                    val ratio = contrastRatio(colors.onContainer, bubble)
                    assertTrue("$condition dark=$isDark day=$isDay ratio=$ratio", ratio >= 4.5)
                }
            }
        }
    }
}

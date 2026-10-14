package io.github.rojhatince47.weatherchan.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherBackgroundPaletteTest {

    private fun contrastRatio(a: Color, b: Color): Double {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun allStates(): List<WeatherBackgroundPalette> =
        WeatherCondition.entries.flatMap { condition ->
            listOf(false, true).flatMap { isDark ->
                listOf(true, false).map { isDay ->
                    WeatherBackgroundPalette.of(condition, isDark = isDark, isDay = isDay)
                }
            }
        }

    @Test
    fun `her durum tanimli ve duraklar farkli`() {
        val states = allStates()
        assertEquals(7 * 2 * 2, states.size)
        states.forEach { palette ->
            assertTrue("$palette en az 2 durak", palette.gradient.size >= 2)
            assertTrue("$palette ust ve alt durak ayni", palette.gradient.first() != palette.gradient.last())
        }
    }

    @Test
    fun `hafif saganak yagmur paletini paylasir`() {
        listOf(false, true).forEach { isDark ->
            listOf(true, false).forEach { isDay ->
                assertEquals(
                    WeatherBackgroundPalette.of(WeatherCondition.RAIN, isDark = isDark, isDay = isDay),
                    WeatherBackgroundPalette.of(WeatherCondition.LIGHT_SHOWERS, isDark = isDark, isDay = isDay)
                )
            }
        }
    }

    @Test
    fun `content ve gosterge renkleri butun duraklara karsi wcag aa`() {
        allStates().forEach { palette ->
            val colors = listOf(palette.contentColor, palette.indicatorActive, palette.indicatorInactive)
            colors.forEach { color ->
                palette.gradient.forEach { stop ->
                    val ratio = contrastRatio(color, stop)
                    assertTrue("$palette kontrast yetersiz: $ratio", ratio >= 4.5)
                }
            }
        }
    }

    @Test
    fun `acik ve koyu tema gradyanlari farkli`() {
        WeatherCondition.entries.forEach { condition ->
            listOf(true, false).forEach { isDay ->
                val light = WeatherBackgroundPalette.of(condition, isDark = false, isDay = isDay)
                val dark = WeatherBackgroundPalette.of(condition, isDark = true, isDay = isDay)
                assertTrue("$condition tema gradyanlari ayni", light.gradient != dark.gradient)
            }
        }
    }

    @Test
    fun `gece gradyanlari gunduzden koyudur`() {
        WeatherCondition.entries.forEach { condition ->
            listOf(false, true).forEach { isDark ->
                val day = WeatherBackgroundPalette.of(condition, isDark = isDark, isDay = true)
                val night = WeatherBackgroundPalette.of(condition, isDark = isDark, isDay = false)
                assertTrue(
                    "$condition gece duragi gunduzden koyu olmali",
                    night.gradient.first().luminance() < day.gradient.first().luminance()
                )
            }
        }
    }

    @Test
    fun `neutral dekoru bos ve duraklari farkli`() {
        listOf(false, true).forEach { isDark ->
            val neutral = WeatherBackgroundPalette.neutral(isDark)
            assertEquals(null, neutral.decor)
            assertTrue(neutral.gradient.first() != neutral.gradient.last())
        }
    }
}
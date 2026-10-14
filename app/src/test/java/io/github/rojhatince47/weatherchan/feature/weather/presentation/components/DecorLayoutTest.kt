package io.github.rojhatince47.weatherchan.feature.weather.presentation.components

import androidx.compose.ui.geometry.Size
import io.github.rojhatince47.weatherchan.core.ui.theme.DecorFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecorLayoutTest {

    private val size = Size(1080f, 2400f)

    private fun layout(family: DecorFamily, isDay: Boolean) = decorLayout(family, size, isDay)

    @Test
    fun `envanter sayilari tabloya uygun`() {
        // Spec §3.2 tablosu birebir (aile x isDay tam matris).
        fun c(family: DecorFamily, isDay: Boolean) = layout(family, isDay).clouds.size
        fun s(family: DecorFamily, isDay: Boolean) = layout(family, isDay).streaks.size
        assertEquals(1, c(DecorFamily.CLEAR, true))
        assertEquals(1, c(DecorFamily.CLEAR, false))
        assertEquals(3, c(DecorFamily.PARTLY_CLOUDY, true))
        assertEquals(3, c(DecorFamily.PARTLY_CLOUDY, false))
        assertEquals(4, c(DecorFamily.CLOUDY, true))
        assertEquals(4, c(DecorFamily.CLOUDY, false))
        assertEquals(3, c(DecorFamily.RAIN, true))
        assertEquals(3, c(DecorFamily.RAIN, false))
        assertEquals(3, c(DecorFamily.THUNDERSTORM, true))
        assertEquals(3, c(DecorFamily.THUNDERSTORM, false))
        assertEquals(3, c(DecorFamily.SNOW, true))
        assertEquals(3, c(DecorFamily.SNOW, false))
        assertEquals(0, s(DecorFamily.CLEAR, true))
        assertEquals(0, s(DecorFamily.CLEAR, false))
        assertEquals(0, s(DecorFamily.PARTLY_CLOUDY, true))
        assertEquals(0, s(DecorFamily.PARTLY_CLOUDY, false))
        assertEquals(0, s(DecorFamily.CLOUDY, true))
        assertEquals(0, s(DecorFamily.CLOUDY, false))
        assertEquals(12, s(DecorFamily.RAIN, true))
        assertEquals(12, s(DecorFamily.RAIN, false))
        assertEquals(16, s(DecorFamily.THUNDERSTORM, true))
        assertEquals(16, s(DecorFamily.THUNDERSTORM, false))
        assertEquals(12, s(DecorFamily.SNOW, true))
        assertEquals(12, s(DecorFamily.SNOW, false))
    }

    @Test
    fun `yildiz sayilari`() {
        assertEquals(5, layout(DecorFamily.CLEAR, isDay = false).stars.size)
        assertEquals(3, layout(DecorFamily.PARTLY_CLOUDY, isDay = false).stars.size)
        assertEquals(2, layout(DecorFamily.CLOUDY, isDay = false).stars.size)
        assertEquals(0, layout(DecorFamily.RAIN, isDay = false).stars.size)
        assertEquals(0, layout(DecorFamily.THUNDERSTORM, isDay = false).stars.size)
        assertEquals(0, layout(DecorFamily.SNOW, isDay = false).stars.size)
        assertEquals(0, layout(DecorFamily.CLEAR, isDay = true).stars.size)
    }

    @Test
    fun `gok cismi yalniz acik ve parcali bulutlu`() {
        assertNotNull(layout(DecorFamily.CLEAR, isDay = true).celestial)
        assertNotNull(layout(DecorFamily.CLEAR, isDay = false).celestial)
        assertNotNull(layout(DecorFamily.PARTLY_CLOUDY, isDay = true).celestial)
        assertNotNull(layout(DecorFamily.PARTLY_CLOUDY, isDay = false).celestial)
        assertNull(layout(DecorFamily.CLOUDY, isDay = true).celestial)
        assertNull(layout(DecorFamily.RAIN, isDay = true).celestial)
        assertNull(layout(DecorFamily.SNOW, isDay = false).celestial)
    }

    @Test
    fun `butun konumlar birim karede`() {
        DecorFamily.entries.forEach { family ->
            listOf(true, false).forEach { isDay ->
                val l = layout(family, isDay)
                (listOfNotNull(l.celestial) + l.stars + l.clouds + l.streaks).forEach { p ->
                    assertTrue("$family/$isDay konum disarida: $p", p.x in 0f..1f && p.y in 0f..1f)
                }
            }
        }
    }
}
package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionTextsTest {

    @Test
    fun `negatif ve tasan tohumlar dizi sinirinda kalir`() {
        listOf(1, 3, 5, 8).forEach { size ->
            listOf(Int.MIN_VALUE, -7, -1, 0, 1, 7, Int.MAX_VALUE).forEach { seed ->
                assertTrue("size=$size seed=$seed", lineIndex(size, seed) in 0 until size)
            }
        }
    }
}

package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompanionClipSelectionTest {

    @Test
    fun `yuklenen klip kaynak kimligi uzerinden ornek kimligi dondurur`() {
        val clips = listOf(100, 101, 102)
        val sampleByRes = mapOf(100 to 1, 101 to 2, 102 to 3)

        val sampleId = sampleIdForClip(clips, 1, sampleByRes, loadedSampleIds = setOf(1, 2, 3))

        assertEquals(2, sampleId)
    }

    @Test
    fun `yuklenmeyen klip icin null doner`() {
        val clips = listOf(100, 101, 102)
        val sampleByRes = mapOf(100 to 1, 101 to 2, 102 to 3)

        val sampleId = sampleIdForClip(clips, 1, sampleByRes, loadedSampleIds = setOf(1, 3))

        assertNull(sampleId)
    }

    @Test
    fun `gecersiz indeks icin null doner`() {
        val clips = listOf(100)
        val sampleByRes = mapOf(100 to 1)

        assertNull(sampleIdForClip(clips, 5, sampleByRes, loadedSampleIds = setOf(1)))
    }

    @Test
    fun `ornek kimligi olmayan klip icin null doner`() {
        val clips = listOf(100, 101)
        val sampleByRes = mapOf(100 to 1)

        assertNull(sampleIdForClip(clips, 1, sampleByRes, loadedSampleIds = setOf(1)))
    }
}

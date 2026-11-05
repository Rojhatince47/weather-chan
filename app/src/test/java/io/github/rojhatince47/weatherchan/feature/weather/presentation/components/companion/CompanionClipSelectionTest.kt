package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import org.junit.Assert.assertEquals
import org.junit.Test

class CompanionClipSelectionTest {

    @Test
    fun `yuklenen ornekler kaynak kimligiyle eslesir`() {
        val clips = listOf(100, 101, 102)
        val sampleByRes = mapOf(100 to 1, 101 to 2, 102 to 3)

        val available = availableTapClips(clips, sampleByRes, loadedSampleIds = setOf(1, 2, 3))

        assertEquals(clips, available)
    }

    @Test
    fun `yuklenmeyen klip atlanir`() {
        val clips = listOf(100, 101, 102)
        val sampleByRes = mapOf(100 to 1, 101 to 2, 102 to 3)

        val available = availableTapClips(clips, sampleByRes, loadedSampleIds = setOf(1, 3))

        assertEquals(listOf(100, 102), available)
    }

    @Test
    fun `ornek kimligi olmayan klip atlanir`() {
        val clips = listOf(100, 101)
        val sampleByRes = mapOf(100 to 1)

        val available = availableTapClips(clips, sampleByRes, loadedSampleIds = setOf(1))

        assertEquals(listOf(100), available)
    }
}

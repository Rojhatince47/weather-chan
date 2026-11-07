package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompanionAmbientLoopTest {

    @Test
    fun `ilk klip gecikmeden sonra calar ve araliklarla tekrarlar`() = runTest {
        val player = RecordingPlayer()
        val job = launch {
            runAmbientLoop(
                player = player,
                clip = CompanionAmbientClip.BIRDS,
                firstDelayMs = 1_200,
                nextIntervalMs = { 20_000 }
            )
        }

        advanceTimeBy(1_199)
        assertTrue(player.played.isEmpty())

        advanceTimeBy(2)
        assertEquals(listOf(CompanionAmbientClip.BIRDS), player.played)

        advanceTimeBy(19_999)
        assertEquals(1, player.played.size)

        advanceTimeBy(2)
        assertEquals(2, player.played.size)

        job.cancel()
        advanceTimeBy(1)
        assertEquals(2, player.stops)
    }

    @Test
    fun `iptal edilince calan ses durdurulur`() = runTest {
        val player = RecordingPlayer()
        val job = launch {
            runAmbientLoop(
                player = player,
                clip = CompanionAmbientClip.RAIN,
                firstDelayMs = 0,
                nextIntervalMs = { 30_000 }
            )
        }

        advanceTimeBy(1)
        assertEquals(1, player.played.size)
        assertEquals(1, player.stops) // döngü başında önceki sesi keser

        job.cancel()
        advanceTimeBy(1)

        assertEquals(2, player.stops) // iptalde çalanı durdurur
    }

    private class RecordingPlayer : CompanionAmbientPlayer {
        val played = mutableListOf<CompanionAmbientClip>()
        var stops = 0

        override fun play(clip: CompanionAmbientClip) {
            played += clip
        }

        override fun stop() {
            stops++
        }

        override fun release() = Unit
    }
}

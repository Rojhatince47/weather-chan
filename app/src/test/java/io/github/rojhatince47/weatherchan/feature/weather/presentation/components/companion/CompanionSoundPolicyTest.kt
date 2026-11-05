package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionSoundPolicyTest {

    @Test
    fun `ayar kapaliyken ses calinmaz`() {
        assertFalse(shouldPlayCompanionSound(enabled = false, silentMode = false))
    }

    @Test
    fun `sessiz modda ses calinmaz`() {
        assertFalse(shouldPlayCompanionSound(enabled = true, silentMode = true))
    }

    @Test
    fun `ayar acik ve normal modda ses calinir`() {
        assertTrue(shouldPlayCompanionSound(enabled = true, silentMode = false))
    }
}

package io.github.rojhatince47.weatherchan.core.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherAmbienceTest {
    @Test
    fun `ambiyans tokenlari kilitli`() {
        assertEquals(40_000, WeatherAmbience.CLOUD_DRIFT_FAST_MS)
        assertEquals(65_000, WeatherAmbience.CLOUD_DRIFT_MID_MS)
        assertEquals(90_000, WeatherAmbience.CLOUD_DRIFT_SLOW_MS)
        assertEquals(2_500, WeatherAmbience.RAIN_FALL_MS)
        assertEquals(8_000, WeatherAmbience.SNOW_FALL_MS)
        assertEquals(3_000, WeatherAmbience.TWINKLE_MS)
        assertEquals(8_000, WeatherAmbience.LIGHTNING_MIN_MS)
        assertEquals(20_000, WeatherAmbience.LIGHTNING_MAX_MS)
    }
}

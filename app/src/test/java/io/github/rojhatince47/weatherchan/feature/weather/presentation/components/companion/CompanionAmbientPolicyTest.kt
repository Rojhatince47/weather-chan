package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test

class CompanionAmbientPolicyTest {

    @Test
    fun `acik havada gunduz kus sesi`() {
        assertEquals(
            CompanionAmbientClip.BIRDS,
            ambientClipFor(WeatherCondition.CLEAR, isDay = true)
        )
    }

    @Test
    fun `acik havada gece cekirge sesi`() {
        assertEquals(
            CompanionAmbientClip.CRICKETS,
            ambientClipFor(WeatherCondition.CLEAR, isDay = false)
        )
    }

    @Test
    fun `bulutlu havada ruzgar sesi`() {
        assertEquals(CompanionAmbientClip.WIND, ambientClipFor(WeatherCondition.CLOUDY, isDay = true))
        assertEquals(CompanionAmbientClip.WIND, ambientClipFor(WeatherCondition.PARTLY_CLOUDY, isDay = true))
    }

    @Test
    fun `yagmurda yagmur sesi`() {
        assertEquals(CompanionAmbientClip.RAIN, ambientClipFor(WeatherCondition.RAIN, isDay = true))
        assertEquals(CompanionAmbientClip.RAIN, ambientClipFor(WeatherCondition.LIGHT_SHOWERS, isDay = true))
    }

    @Test
    fun `karda kar sesi`() {
        assertEquals(CompanionAmbientClip.SNOW, ambientClipFor(WeatherCondition.SNOW, isDay = true))
    }

    @Test
    fun `firtinada firtina sesi`() {
        assertEquals(CompanionAmbientClip.STORM, ambientClipFor(WeatherCondition.THUNDERSTORM, isDay = true))
    }
}

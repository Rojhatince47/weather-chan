package io.github.rojhatince47.weatherchan.feature.settings.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `system cycles to light`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.SYSTEM.next())
    }

    @Test
    fun `light cycles to dark`() {
        assertEquals(ThemeMode.DARK, ThemeMode.LIGHT.next())
    }

    @Test
    fun `dark cycles back to system`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.DARK.next())
    }

    @Test
    fun `full cycle returns to start`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.SYSTEM.next().next().next())
    }
}

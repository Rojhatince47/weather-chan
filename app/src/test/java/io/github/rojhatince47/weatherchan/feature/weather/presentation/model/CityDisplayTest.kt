package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CityDisplayTest {

    @Test
    fun `yerlesim ve bolge birlestirilir`() {
        assertEquals("Derik, Mardin", displayTitle("Derik", "Mardin", true))
    }

    @Test
    fun `ayni ad tekrarlanmaz`() {
        assertEquals("Mardin", displayTitle("Mardin", "Mardin", true))
    }

    @Test
    fun `yalniz yerlesim adi kullanilir`() {
        assertEquals("Derik", displayTitle("Derik", null, true))
    }

    @Test
    fun `yerlesim yoksa bolge kullanilir`() {
        assertEquals("Mardin", displayTitle("", "Mardin", true))
    }

    @Test
    fun `ikisi de yoksa bos doner`() {
        assertEquals("", displayTitle("", null, true))
    }

    @Test
    fun `normal sehir adi aynen doner`() {
        assertEquals("İstanbul", displayTitle("İstanbul", "Marmara", false))
    }
}

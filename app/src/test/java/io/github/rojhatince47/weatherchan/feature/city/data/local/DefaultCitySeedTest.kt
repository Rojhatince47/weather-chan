package io.github.rojhatince47.weatherchan.feature.city.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCitySeedTest {

    private val cities = DefaultCitySeed.cities

    @Test
    fun `bos degil`() {
        assertTrue("Gömülü liste boş olamaz", cities.isNotEmpty())
    }

    @Test
    fun `id'ler cakismaz`() {
        val ids = cities.map { it.id }
        assertEquals("Aynı koordinata sahip iki şehir var: $ids", ids.size, ids.toSet().size)
    }

    @Test
    fun `isimler cakismaz`() {
        val names = cities.map { it.name.lowercase() }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `tum koordinatlar Turkiye sinirlari icinde`() {
        // Türkiye: 35.8–42.1 kuzey, 25.7–44.9 doğu
        cities.forEach { city ->
            val lat = city.coordinates.latitude
            val lon = city.coordinates.longitude
            assertTrue("${city.name} enlemi Türkiye dışında: $lat", lat in 35.8..42.1)
            assertTrue("${city.name} boylamı Türkiye dışında: $lon", lon in 25.7..44.9)
        }
    }

    @Test
    fun `bes buyuk sehir mutlaka var`() {
        val isimler = cities.map { it.name }.toSet()
        assertTrue(isimler.containsAll(setOf("İstanbul", "Ankara", "İzmir", "Bursa", "Antalya")))
    }
}

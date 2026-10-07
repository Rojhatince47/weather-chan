package io.github.rojhatince47.weatherchan.feature.weather.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CityProximityTest {

    private fun city(lat: Double, lon: Double, name: String = "X") =
        City(name = name, region = null, country = null, coordinates = Coordinates(lat, lon))

    @Test
    fun `gercek cihaz cifti ayni yerdir`() {
        // Konum "Mardin" (GPS, 7 basamak) ↔ arama "Derik" (geocoding,
        // 5 basamak): ~330 m — kimlikleri farklı, yerleri aynı.
        assertTrue(CityProximity.isSamePlace(city(37.3652283, 40.2676315, "Mardin"), city(37.36336, 40.26473, "Derik")))
    }

    @Test
    fun `esik alti ayni yerdir`() {
        // (37.0, 40.0) → 0.089° kuzey ≈ 9.90 km (< 10).
        assertTrue(CityProximity.isSamePlace(city(37.0, 40.0), city(37.089, 40.0)))
    }

    @Test
    fun `esik ustu farkli yerdir`() {
        // (37.0, 40.0) → 0.091° kuzey ≈ 10.12 km (≥ 10).
        assertFalse(CityProximity.isSamePlace(city(37.0, 40.0), city(37.091, 40.0)))
    }

    @Test
    fun `uzak ayni isimli sehirler farklidir`() {
        // Kullanıcının kasıtlı üç "Derik" eklemesi: Muş, Van, Mardin.
        val mus = city(39.11667, 42.38333, "Derik")
        val van = city(38.58333, 43.51667, "Derik")
        val mardin = city(37.36336, 40.26473, "Derik")
        assertFalse(CityProximity.isSamePlace(mus, van))
        assertFalse(CityProximity.isSamePlace(mus, mardin))
        assertFalse(CityProximity.isSamePlace(van, mardin))
    }

    @Test
    fun `ayni nokta ayni yerdir`() {
        assertTrue(CityProximity.isSamePlace(city(41.0082, 28.9784), city(41.0082, 28.9784)))
    }

    @Test
    fun `kutuplarda ve antimeridyanda NaN uretmez`() {
        assertFalse(CityProximity.distanceKm(Coordinates(90.0, 180.0), Coordinates(-90.0, -180.0)).isNaN())
    }
}

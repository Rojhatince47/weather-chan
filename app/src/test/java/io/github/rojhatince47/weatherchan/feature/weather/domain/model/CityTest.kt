package io.github.rojhatince47.weatherchan.feature.weather.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CityTest {

    private fun city(name: String = "Bursa", latitude: Double = 40.19, longitude: Double = 29.06) = City(
        name = name,
        region = "Marmara",
        country = "Türkiye",
        coordinates = Coordinates(latitude = latitude, longitude = longitude)
    )

    @Test
    fun `ayni koordinatlar ayni id uretir`() {
        val a = city()
        val b = city(name = "Bursa Kalesi")
        assertEquals(a.id, b.id)
    }

    @Test
    fun `farkli koordinatlar farkli id uretir`() {
        val a = city(latitude = 40.19, longitude = 29.06)
        val b = city(latitude = 40.20, longitude = 29.06)
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun `id gomulu listeden ve geocodingden ayni gelir`() {
        // Aynı şehir, iki farklı kaynaktan; id'ler eşleşmeli (mükerrer eklemeyi önler)
        val gomulu = city()
        val geocoding = city(name = "BURSA")
        assertEquals(gomulu.id, geocoding.id)
    }

    @Test
    fun `iki yakin yerlesim ayni id'ye dusmez`() {
        // ~110 m çözünürlük: 0.0005 derece fark ayrıştırılabilmeli
        val a = city(latitude = 40.1900, longitude = 29.0600)
        val b = city(latitude = 40.1905, longitude = 29.0600)
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun `dogu ve bati yarimkure id cakismaz`() {
        val dogu = city(latitude = 51.51, longitude = 0.13) // Londra
        val bati = city(latitude = 40.71, longitude = -74.01) // New York
        assertNotEquals(dogu.id, bati.id)
    }

    @Test
    fun `yuvarlama yarim noktada yukari gider`() {
        val a = city(latitude = 40.19049, longitude = 29.06)
        val b = city(latitude = 40.19050, longitude = 29.06)
        assertNotEquals(a.id, b.id)
    }
}

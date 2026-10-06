package io.github.rojhatince47.weatherchan.feature.city.domain.suggestion

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CitySeedTest {
    @Test
    fun `bos ulke adi nulla normalize edilir`() {
        // Overpass kayıtları boş dize taşır (spec §4); alt satır boş " · " göstermemeli
        val city = CitySeed("Kuzguncuk", "", "", Coordinates(41.03, 29.03)).toCity()
        assertNull(city.country)
        assertEquals("Kuzguncuk", city.name)
    }
}

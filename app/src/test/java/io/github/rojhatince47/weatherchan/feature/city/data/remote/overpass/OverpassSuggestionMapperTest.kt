package io.github.rojhatince47.weatherchan.feature.city.data.remote.overpass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassSuggestionMapperTest {
    private val mapper = OverpassSuggestionMapper()

    private fun element(tags: Map<String, String>, lat: Double, lon: Double) =
        OverpassElementDto(type = "node", lat = lat, lon = lon, tags = tags)

    @Test
    fun `ingilizce tercihinde nameEn kullanilir name yedektir`() {
        val node = element(tags = mapOf("name" to "İstanbul", "name:en" to "Istanbul", "place" to "city"), lat = 41.0, lon = 29.0)
        val seeds = mapper.toSeeds(listOf(node), preferEnglish = true)
        assertEquals("Istanbul", seeds.single().name)
    }

    @Test
    fun `turkce tercihinde yerel ad kullanilir`() {
        val node = element(tags = mapOf("name" to "İstanbul", "name:en" to "Istanbul", "place" to "city"), lat = 41.0, lon = 29.0)
        assertEquals("İstanbul", mapper.toSeeds(listOf(node), preferEnglish = false).single().name)
    }

    @Test
    fun `adi olmayan dugum elenir`() {
        // C1: name etiketi yoksa kayıt önerilemez
        val node = element(tags = mapOf("place" to "city"), lat = 41.0, lon = 29.0)
        assertTrue(mapper.toSeeds(listOf(node), preferEnglish = false).isEmpty())
    }

    @Test
    fun `bos ulke alanlari tasinir`() {
        val node = element(tags = mapOf("name" to "Kuzguncuk", "place" to "town"), lat = 41.03, lon = 29.03)
        val seed = mapper.toSeeds(listOf(node), preferEnglish = false).single()
        assertEquals("", seed.countryCode)
        assertEquals("", seed.countryName) // CitySeed.toCity null'a normalize eder
    }
}

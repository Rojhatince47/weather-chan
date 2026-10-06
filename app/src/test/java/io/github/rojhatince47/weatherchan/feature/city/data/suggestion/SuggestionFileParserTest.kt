package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuggestionFileParserTest {

    private val valid = """
        {
          "version": 2,
          "worldFallbackOrder": ["GB"],
          "cities": [
            { "name": "Ankara", "code": "TR", "country": "Türkiye", "lat": 39.93, "lon": 32.86 }
          ]
        }
    """.trimIndent()

    @Test
    fun `gecerli dosya ayristirilir`() {
        val data = SuggestionFileParser.parse(valid)!!
        assertEquals(listOf("GB"), data.worldFallbackOrder)
        assertEquals("Ankara", data.cities.single().name)
        assertEquals("TR", data.cities.single().countryCode)
        assertEquals(39.93, data.cities.single().coordinates.latitude, 0.0001)
    }

    @Test
    fun `desteklenmeyen surum reddedilir`() {
        assertNull(SuggestionFileParser.parse(valid.replace("\"version\": 2", "\"version\": 1")))
    }

    @Test
    fun `bozuk metin istisna firlatmaz`() {
        assertNull(SuggestionFileParser.parse("{ bozuk"))
    }

    @Test
    fun `bos ve null metin reddedilir`() {
        assertNull(SuggestionFileParser.parse(null))
        assertNull(SuggestionFileParser.parse("   "))
    }

    @Test
    fun `eksik alanli kayit dosyayi gecersiz kilar`() {
        // Sema varsayilansizdir (bkz. Task 1): eksik alan decode hatasidir,
        // sessizce varsayilana dusmez. Parser tum dosyayi reddeder.
        val json = valid.replace("\"code\": \"TR\", ", "")
        assertNull(SuggestionFileParser.parse(json))
    }

    @Test
    fun `alani dolu ama gecersiz kayit elenir`() {
        // Alan VAR ama icerigi gecersiz: boyle kayit dosyayi bozmaz,
        // parser'in suzgeci tek kaydi dusurur.
        val json = valid.replace("\"name\": \"Ankara\"", "\"name\": \" \"")
        assertEquals(0, SuggestionFileParser.parse(json)!!.cities.size)
    }

    @Test
    fun `iki harften uzun ulke kodlu kayit elenir`() {
        val json = valid.replace("\"code\": \"TR\"", "\"code\": \"TUR\"")
        assertEquals(0, SuggestionFileParser.parse(json)!!.cities.size)
    }

    @Test
    fun `iki harften uzun kod dunya sirasindan elenir`() {
        val json = valid.replace("\"worldFallbackOrder\": [\"GB\"]", "\"worldFallbackOrder\": [\"GB\", \"TUR\"]")
        assertEquals(listOf("GB"), SuggestionFileParser.parse(json)!!.worldFallbackOrder)
    }

    @Test
    fun `bilinmeyen alanlar hata vermez`() {
        val json = valid.replaceFirst("{", "{ \"yeniAlan\": 5,")
        assertEquals(1, SuggestionFileParser.parse(json)!!.cities.size)
    }
}

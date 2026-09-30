package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `assets/suggestions.json` dosyasının bütünlüğünü korur.
 *
 * Dosya elle yazıldığı için (ağdan indirilmez) en olası hata bozuk veridir:
 * eksik alan, yanlış koordinat, aynı kodda iki farklı ülke adı. Bu testler
 * öneri motoruna ulaşmadan yakalar.
 */
class SuggestionDataIntegrityTest {

    private val file = File("src/main/assets/suggestions.json")

    private fun parse(): SuggestionFileDto {
        assertTrue("Veri dosyası bulunamadı: ${file.absolutePath}", file.isFile)
        return Json { ignoreUnknownKeys = true }
            .decodeFromString<SuggestionFileDto>(file.readText())
    }

    @Test
    fun `dosya parse edilebiliyor ve surum destekleniyor`() {
        assertEquals(1, parse().version)
    }

    @Test
    fun `kayitlar ozgun`() {
        val keys = parse().cities.map { "${it.name}|${it.code}|${it.lat}|${it.lon}" }
        assertEquals("Aynı şehir iki kez yazılmış: ${keys.groupingBy { it }.eachCount().filter { it.value > 1 }.keys}",
            keys.size, keys.distinct().size)
    }

    @Test
    fun `ayni koordinat farkli isimlerde tekrarlamiyor`() {
        // `City.id` koordinattan türetilir: aynı koordinata yazılmış iki şehir
        // tek bir kart olur ve öneride ikisi birden görünmez.
        val byCoordinates = parse().cities.groupBy({ "${it.lat}|${it.lon}" }, { it.name })
        val clashes = byCoordinates.filterValues { it.size > 1 }
        assertTrue("Aynı koordinatta birden fazla şehir: $clashes", clashes.isEmpty())
    }

    @Test
    fun `koordinatlar gecerli aralikta`() {
        parse().cities.forEach { city ->
            assertTrue("${city.name} enlem geçersiz: ${city.lat}", city.lat in -90.0..90.0)
            assertTrue("${city.name} boylam geçersiz: ${city.lon}", city.lon in -180.0..180.0)
        }
    }

    @Test
    fun `ayni ulke kodu her zaman ayni adi tasiyor`() {
        val namesByCode = parse().cities.groupBy({ it.code }, { it.country })
        val clashes = namesByCode.filterValues { it.distinct().size > 1 }
        assertTrue("Kod/ad çakışması: $clashes", clashes.isEmpty())
    }

    @Test
    fun `her sehirin adi ve ulke kodu dolu`() {
        parse().cities.forEach { city ->
            assertTrue("Boş alan: $city", city.name.isNotBlank() && city.code.length == 2 && city.country.isNotBlank())
        }
    }

    @Test
    fun `dunya yedegi yalniz veride olan ulkeleri iceriyor`() {
        val data = parse()
        val codes = data.cities.map { it.code }.toSet()
        val unknown = data.worldFallbackOrder.filterNot { it in codes }
        assertTrue("Veride olmayan ülke kodu: $unknown", unknown.isEmpty())
    }

    @Test
    fun `turkiye ve her dunya yedegi ulkesi en az sekiz sehir iceriyor`() {
        val data = parse()
        listOf("TR").plus(data.worldFallbackOrder).distinct().forEach { code ->
            assertTrue("$code ülkesinden en az 8 şehir gerekli", data.cities.count { it.code == code } >= 8)
        }
    }
}
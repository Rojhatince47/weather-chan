package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import java.io.File
import kotlin.math.roundToLong
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `assets/suggestions.json` dosyasının bütünlüğünü korur.
 *
 * Dosya elle yazıldığı için (ağdan indirilmez) en olası hata bozuk veridir:
 * eksik alan, yanlış koordinat, aynı kodda iki farklı ülke adı. Bu testler
 * öneri motoruna ulaşmadan yakalar. Eksik alan, varsayılanı olmayan şema
 * sayesinde decode hatası olarak yükselir; sessizce varsayılana düşmez.
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
        val data = parse()
        assertEquals(2, data.version)
        // Boş sıra, dünya dalını sessizce öldürür: ülkeler testi boş listeyle
        // kendiliğinden geçerdi. Sıra dolu ve tekrarsız olmalı.
        assertTrue("worldFallbackOrder boş olamaz", data.worldFallbackOrder.isNotEmpty())
        assertEquals(
            "worldFallbackOrder içinde tekrar: ${data.worldFallbackOrder.groupingBy { it }.eachCount().filter { it.value > 1 }.keys}",
            data.worldFallbackOrder.size,
            data.worldFallbackOrder.distinct().size
        )
    }

    @Test
    fun `kayitlar ozgun`() {
        val keys = parse().cities.map { "${it.name}|${it.code}|${it.lat}|${it.lon}" }
        assertEquals(
            "Aynı şehir iki kez yazılmış: ${keys.groupingBy { it }.eachCount().filter { it.value > 1 }.keys}",
            keys.size,
            keys.distinct().size
        )
    }

    @Test
    fun `ayni koordinat farkli isimlerde tekrarlamiyor`() {
        // `City.id` koordinatı 3 basamağa yuvarlar (~110 m): aynı id'li iki şehir
        // tek kart olur ve öneride ikisi birden görünmez. Gruplama da aynı
        // yuvarlamayla yapılır — birebir eşleşme, yuvarlama nedeniyle çakışan
        // kayıtları görmezden gelirdi.
        fun id(lat: Double, lon: Double): Long = (lat * 1_000).roundToLong() * 10_000_000 + (lon * 1_000).roundToLong()

        val byId = parse().cities.groupBy({ id(it.lat, it.lon) }, { it.name })
        val clashes = byId.filterValues { it.size > 1 }
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
    fun `her kayitin adi ulke kodu ve ulke adi dolu`() {
        parse().cities.forEach { city ->
            assertTrue(
                "Boş ya da bozuk alan: $city",
                city.name.isNotBlank() &&
                    city.code.matches(Regex("[A-Z]{2}")) &&
                    city.country.isNotBlank()
            )
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
        // İlk eksik ülkede durmak yerine hepsini tek mesajda topla: dünya
        // verisi genişlerken eksiklerin tamamı bir koşuda görünür.
        val insufficient = listOf("TR").plus(data.worldFallbackOrder).distinct()
            .filter { code -> data.cities.count { it.code == code } < 8 }
        assertTrue("En az 8 şehir gerekli olan ülkelerde eksik var: $insufficient", insufficient.isEmpty())
    }

    @Test
    fun `ingilizce cevriler tamdir`() {
        // Spec §6: nameEn/countryEn opsiyonel ama doluluk %100 kilitlidir —
        // eksik çeviri testi düşürür, sessizce Türkçe'ye dönerken gizli kalmaz.
        val data = parse()
        val missingName = data.cities.filter { it.nameEn.isNullOrBlank() }.map { it.name }
        assertTrue("nameEn eksik/boş: $missingName", missingName.isEmpty())
        val missingCountry = data.cities.filter { it.countryEn.isNullOrBlank() }.map { "${it.code}/${it.country}" }
        assertTrue("countryEn eksik/boş: $missingCountry", missingCountry.isEmpty())
        // Aynı ülke kodu için countryEn tekil olmalı: biri çevrilmiş, diğeri
        // unutulmuş kayıtlar aynı satırda iki farklı ülke adı gösterirdi.
        val enByCode = data.cities.groupBy({ it.code }, { it.countryEn })
        val clashes = enByCode.filterValues { it.distinct().size > 1 }
        assertTrue("Kod/countryEn çakışması: $clashes", clashes.isEmpty())
    }
}

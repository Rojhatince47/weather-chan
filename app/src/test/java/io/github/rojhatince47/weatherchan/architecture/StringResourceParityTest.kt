package io.github.rojhatince47.weatherchan.architecture

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Çeviri eksikleri derleme hatası vermez; eksik anahtar sessizce İngilizce
 * default'a düşer (spec §9/§10). Bu test iki kaynak dosyanın anahtar
 * kümelerinin eşit olduğunu sayar — LayerDependencyTest'in dosya okuma
 * deseniyle aynı yaklaşım (çalışma dizini app modülüdür).
 */
class StringResourceParityTest {

    @Test
    fun `anahtar kumeleri iki kaynak dosyada esittir`() {
        val default = keysOf("src/main/res/values/strings.xml")
        val turkish = keysOf("src/main/res/values-tr/strings.xml")
        assertEquals(
            "values-tr'de eksik/fazla anahtar var (eksik olanlar İngilizce görünür)",
            default,
            turkish
        )
    }

    @Test
    fun `dizi anahtar kumeleri iki kaynak dosyada esittir`() {
        assertEquals(
            "values-tr'de eksik/fazla dizi var",
            arrayKeysOf("src/main/res/values/strings.xml"),
            arrayKeysOf("src/main/res/values-tr/strings.xml")
        )
    }

    private fun keysOf(path: String): Set<String> = File(path)
        .takeIf { it.isFile }?.readLines().orEmpty()
        .mapNotNull { Regex("<string name=\"([^\"]+)\"").find(it)?.groupValues?.get(1) }
        .toSet()

    private fun arrayKeysOf(path: String): Set<String> = File(path)
        .takeIf { it.isFile }?.readText().orEmpty()
        .let { text ->
            Regex("<string-array name=\"([^\"]+)\"")
                .findAll(text)
                .map { match -> match.groupValues[1] }
                .toSet()
        }
}

package io.github.rojhatince47.weatherchan.architecture

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Clean Architecture bağımlılık kurallarını kaynak kod üzerinden denetler.
 * Tek modüllü yapıda katman sınırlarını derleyici değil bu test korur.
 */
class LayerDependencyTest {

    private val sourceRoot = File("src/main/java/io/github/rojhatince47/weatherchan")

    private val frameworkImports = listOf(
        "import android.",
        "import androidx.",
        "import retrofit2.",
        "import okhttp3.",
        "import kotlinx.serialization.",
        "import dagger.hilt.android."
    )

    @Test
    fun `source root is reachable`() {
        assertTrue("Kaynak dizini bulunamadı: ${sourceRoot.absolutePath}", sourceRoot.isDirectory)
    }

    @Test
    fun `domain layer does not depend on frameworks`() {
        val violations = importsIn(layer = "domain").filter { (_, line) ->
            frameworkImports.any { line.startsWith(it) }
        }
        assertNoViolations("domain katmanı framework'e bağımlı olamaz", violations)
    }

    @Test
    fun `domain layer does not depend on data or presentation`() {
        val violations = importsIn(layer = "domain").filter { (_, line) ->
            line.contains(".data.") || line.contains(".presentation.")
        }
        assertNoViolations("domain katmanı dış katmanları bilemez", violations)
    }

    @Test
    fun `presentation layer does not depend on data layer`() {
        val violations = importsIn(layer = "presentation").filter { (_, line) -> line.contains(".data.") }
        assertNoViolations("presentation katmanı data katmanını bilemez", violations)
    }

    private fun importsIn(layer: String): List<Pair<String, String>> = sourceRoot.walkTopDown()
        .filter { it.isFile && it.extension == "kt" && "/$layer/" in it.invariantSeparatorsPath }
        .flatMap { file ->
            file.readLines()
                .map(String::trim)
                .filter { it.startsWith("import ") }
                .map { file.name to it }
        }
        .toList()

    private fun assertNoViolations(rule: String, violations: List<Pair<String, String>>) {
        assertTrue(
            "$rule:\n" + violations.joinToString("\n") { (file, line) -> "  $file → $line" },
            violations.isEmpty()
        )
    }
}

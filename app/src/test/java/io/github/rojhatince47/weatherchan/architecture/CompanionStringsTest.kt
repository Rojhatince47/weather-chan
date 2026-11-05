package io.github.rojhatince47.weatherchan.architecture

import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceCategory
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose
import java.io.File
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionStringsTest {

    private val paths = listOf(
        "src/main/res/values/strings.xml",
        "src/main/res/values-tr/strings.xml"
    )

    @Test
    fun `her kategori dizisi iki dilde uc ile bes arasi rakamsiz satir tasir`() {
        paths.forEach { path ->
            val arrays = arraysOf(path)
            AdviceCategory.entries.forEach { category ->
                val key = "advice_${category.name.lowercase(Locale.ROOT)}"
                val items = arrays[key] ?: error("$path: $key dizisi yok")
                assertTrue("$path/$key 3..5 satir", items.size in 3..5)
                items.forEach {
                    assertTrue("$path/$key bos satir", it.isNotBlank())
                    assertFalse("$path/$key rakam iceriyor: $it", it.contains(Regex("[0-9]")))
                    assertTrue("$path/$key cok uzun: $it", it.length <= 90)
                }
            }
        }
    }

    @Test
    fun `dokunus havuzlari iki dilde dolu ve rakamsiz`() {
        val keys = listOf(
            "companion_tap_surprised",
            "companion_tap_playful",
            "companion_tap_soft"
        )
        paths.forEach { path ->
            val arrays = arraysOf(path)
            keys.forEach { key ->
                val items = arrays[key] ?: error("$path: $key dizisi yok")
                assertTrue("$path/$key 1..5", items.size in 1..5)
                items.forEach {
                    assertTrue("$path/$key bos satir", it.isNotBlank())
                    assertFalse("$path/$key rakam iceriyor: $it", it.contains(Regex("[0-9]")))
                    assertTrue("$path/$key cok uzun: $it", it.length <= 90)
                }
            }
        }
    }

    @Test
    fun `karakter ve poz anahtarlari iki dilde vardir`() {
        val required = buildSet {
            add("companion_character_name")
            add("companion_tap_action")
            add("companion_art_description")
            CompanionPose.entries.forEach { add("companion_pose_${it.name.lowercase(Locale.ROOT)}") }
        }
        paths.forEach { path ->
            val strings = stringsOf(path)
            required.forEach { key ->
                val value = strings[key] ?: error("$path: $key yok")
                assertTrue("$path/$key bos", value.isNotBlank())
            }
            assertTrue(
                "$path: art description %1\$s icermeli",
                strings.getValue("companion_art_description").contains("%1\$s")
            )
        }
    }

    private fun arraysOf(path: String): Map<String, List<String>> {
        val text = File(path).readText()
        return Regex("<string-array name=\"([^\"]+)\"[^>]*>(.*?)</string-array>", RegexOption.DOT_MATCHES_ALL)
            .findAll(text)
            .associate { match ->
                val items = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(match.groupValues[2])
                    .map { it.groupValues[1].trim() }
                    .toList()
                match.groupValues[1] to items
            }
    }

    private fun stringsOf(path: String): Map<String, String> = File(path).readLines()
        .mapNotNull { line ->
            Regex("<string name=\"([^\"]+)\"[^>]*>(.*?)</string>").find(line)?.let {
                it.groupValues[1] to it.groupValues[2].trim()
            }
        }
        .toMap()
}

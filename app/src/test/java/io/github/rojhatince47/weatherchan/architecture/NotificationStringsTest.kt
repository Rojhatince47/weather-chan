package io.github.rojhatince47.weatherchan.architecture

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationStringsTest {

    private val paths = listOf(
        "src/main/res/values/strings.xml",
        "src/main/res/values-tr/strings.xml"
    )

    private val requiredStrings = listOf(
        "notif_channel_alerts",
        "notif_channel_summary",
        "notif_channel_companion",
        "notif_title_severe",
        "notif_title_rain",
        "notif_title_summary",
        "notif_title_companion",
        "notif_severe_storm",
        "notif_severe_snow",
        "notif_severe_wind",
        "notif_severe_cold",
        "notif_severe_hot",
        "notif_rain_soon",
        "notif_summary"
    )

    @Test
    fun `bildirim metinleri iki dilde dolu`() {
        paths.forEach { path ->
            val strings = stringsOf(path)
            requiredStrings.forEach { key ->
                val value = strings[key] ?: error("$path: $key yok")
                assertTrue("$path/$key bos", value.isNotBlank())
                assertTrue("$path/$key cok uzun", value.length <= 140)
            }
        }
    }

    @Test
    fun `ozet metni dort yer tutucu tasir`() {
        paths.forEach { path ->
            val summary = stringsOf(path).getValue("notif_summary")
            listOf("%1\$s", "%2\$d", "%3\$d", "%4\$d").forEach { placeholder ->
                assertTrue("$path: notif_summary $placeholder icermeli", summary.contains(placeholder))
            }
        }
    }

    @Test
    fun `uyari metinleri rakam icermez`() {
        val keys = requiredStrings.filter {
            it.startsWith("notif_severe_") || it == "notif_rain_soon"
        }
        paths.forEach { path ->
            val strings = stringsOf(path)
            keys.forEach { key ->
                val value = strings.getValue(key)
                assertTrue("$path/$key rakam iceriyor: $value", !value.contains(Regex("[0-9]")))
            }
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

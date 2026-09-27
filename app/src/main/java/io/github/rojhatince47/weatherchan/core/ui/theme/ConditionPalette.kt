package io.github.rojhatince47.weatherchan.core.ui.theme

import androidx.compose.ui.graphics.Color
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition

/** Bir hava koşulunun yüzey, metin ve vurgu renkleri. */
data class ConditionColors(
    val container: Color,
    val onContainer: Color,
    val accent: Color
)

/**
 * Hava koşuluna göre renk üretir. Saf fonksiyondur; test edilebilir ve
 * ekranlardan bağımsızdır.
 */
object ConditionPalette {

    private val light: Map<WeatherCondition, ConditionColors> = mapOf(
        WeatherCondition.CLEAR to ConditionColors(
            container = Color(0xFFFFF3C4),
            onContainer = Color(0xFF4A3B00),
            accent = Color(0xFFF2A007)
        ),
        WeatherCondition.PARTLY_CLOUDY to ConditionColors(
            container = Color(0xFFE3F1FC),
            onContainer = Color(0xFF10344A),
            accent = Color(0xFF4FA3D9)
        ),
        WeatherCondition.CLOUDY to ConditionColors(
            container = Color(0xFFE8EBEE),
            onContainer = Color(0xFF2B3136),
            accent = Color(0xFF7A8791)
        ),
        WeatherCondition.RAIN to ConditionColors(
            container = Color(0xFFDCE9F7),
            onContainer = Color(0xFF0B2C4A),
            accent = Color(0xFF2F6FB5)
        ),
        WeatherCondition.SNOW to ConditionColors(
            container = Color(0xFFE9F1F7),
            onContainer = Color(0xFF1B3A4B),
            accent = Color(0xFF6FA8C7)
        ),
        WeatherCondition.THUNDERSTORM to ConditionColors(
            container = Color(0xFFE7E0F7),
            onContainer = Color(0xFF2B1D52),
            accent = Color(0xFF7A5BD6)
        )
    )

    private val dark: Map<WeatherCondition, ConditionColors> = mapOf(
        WeatherCondition.CLEAR to ConditionColors(
            container = Color(0xFF4A3C05),
            onContainer = Color(0xFFFFF0BF),
            accent = Color(0xFFFFC64D)
        ),
        WeatherCondition.PARTLY_CLOUDY to ConditionColors(
            container = Color(0xFF16303F),
            onContainer = Color(0xFFD6EAF8),
            accent = Color(0xFF7FC1E8)
        ),
        WeatherCondition.CLOUDY to ConditionColors(
            container = Color(0xFF33393E),
            onContainer = Color(0xFFDFE4E8),
            accent = Color(0xFFA7B2BA)
        ),
        WeatherCondition.RAIN to ConditionColors(
            container = Color(0xFF123049),
            onContainer = Color(0xFFD2E6F7),
            accent = Color(0xFF6FAEE8)
        ),
        WeatherCondition.SNOW to ConditionColors(
            container = Color(0xFF1B3A48),
            onContainer = Color(0xFFDCEBF3),
            accent = Color(0xFFA2CDE4)
        ),
        WeatherCondition.THUNDERSTORM to ConditionColors(
            container = Color(0xFF2A2150),
            onContainer = Color(0xFFE4DCF8),
            accent = Color(0xFFA78BE8)
        )
    )

    fun of(condition: WeatherCondition, isDark: Boolean): ConditionColors =
        colorsFor(isDark)[condition] ?: error("Koşul paleti tanımsız: $condition")

    fun colorsFor(isDark: Boolean): Map<WeatherCondition, ConditionColors> = if (isDark) dark else light
}

package io.github.rojhatince47.weatherchan.feature.weather.domain.model

/**
 * Görünen yağış olasılığı. Hesaplama 100'e ulaşsa bile arayüzde "kesin"
 * iddiası kurulmaz; tavan %99'dur. Karar eşikleri ham değeri kullanmaya
 * devam eder (uyarı mantığı bundan etkilenmez).
 */
object PrecipitationProbability {
    const val DISPLAY_MAX = 99

    fun forDisplay(value: Int): Int = value.coerceIn(0, DISPLAY_MAX)
}

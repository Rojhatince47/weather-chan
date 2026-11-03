package io.github.rojhatince47.weatherchan.core.ui.theme

/**
 * Ambiyans animasyonlarının süreleri (ms). HavaMotion yalnızca arayüz geçişleri
 * içindir; ambiyans hızları burada tutulur (spec §3.3). Sakinlik aralıkları:
 * bulut 40-90 sn/tur, yağmur ~2,5 sn, kar ~8 sn, titreme ~3 sn, şimşek 8-20 sn.
 */
object WeatherAmbience {
    const val CLOUD_DRIFT_FAST_MS = 40_000
    const val CLOUD_DRIFT_MID_MS = 65_000
    const val CLOUD_DRIFT_SLOW_MS = 90_000
    const val RAIN_FALL_MS = 2_500
    const val SNOW_FALL_MS = 8_000
    const val TWINKLE_MS = 3_000
    const val LIGHTNING_MIN_MS = 8_000
    const val LIGHTNING_MAX_MS = 20_000
}

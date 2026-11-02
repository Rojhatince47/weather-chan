package io.github.rojhatince47.weatherchan.core.ui.theme

/**
 * Weather-chan karakterinin uzun süreleri. [HavaMotion] arayüz geçişleri
 * içindir (100–500 ms); buradakiler karakter ambiyansı ve dokunuş tepkisidir.
 *
 * `delay` ve `uptimeMillis` `Long`, `tween` `Int` beklediği için tipler
 * kullanım yerine göre seçilmiştir.
 */
object CompanionMotion {
    const val SYMBOL_MS = 900L
    const val REACTION_MS = 2400L
    const val BREATH_MS = 2600
    const val TAP_DEBOUNCE_MS = 250L
}

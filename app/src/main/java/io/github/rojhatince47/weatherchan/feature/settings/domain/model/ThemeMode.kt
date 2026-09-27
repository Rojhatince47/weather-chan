package io.github.rojhatince47.weatherchan.feature.settings.domain.model

/** Kullanıcının tema tercihi. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    /** Simgeye dokununca sıradaki moda geçer. */
    fun next(): ThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }
}

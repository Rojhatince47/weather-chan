package io.github.rojhatince47.weatherchan.feature.weather.presentation.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import io.github.rojhatince47.weatherchan.core.ui.theme.DecorFamily

/**
 * Dekor sahnesinin birim-kare (0..1) konumları. Saf geometridir; çizim ve
 * animasyon buradan beslenir. Sayılar spec §3.2 tablosuyla kilitlidir.
 */
internal data class DecorLayout(
    val celestial: Offset?,
    val stars: List<Offset>,
    val clouds: List<Offset>,
    val streaks: List<Offset>
)

private val cloudSlots = listOf(
    Offset(0.28f, 0.30f),
    Offset(0.55f, 0.22f),
    Offset(0.75f, 0.38f),
    Offset(0.40f, 0.45f)
)

private val starSlots = listOf(
    Offset(0.15f, 0.12f),
    Offset(0.35f, 0.22f),
    Offset(0.55f, 0.10f),
    Offset(0.88f, 0.35f),
    Offset(0.25f, 0.35f)
)

internal fun decorLayout(family: DecorFamily, size: Size, isDay: Boolean): DecorLayout {
    val cloudCount = when (family) {
        DecorFamily.CLEAR -> 1
        DecorFamily.PARTLY_CLOUDY -> 3
        DecorFamily.CLOUDY -> 4
        DecorFamily.RAIN, DecorFamily.THUNDERSTORM, DecorFamily.SNOW -> 3
    }
    val streakCount = when (family) {
        DecorFamily.RAIN, DecorFamily.SNOW -> 12
        DecorFamily.THUNDERSTORM -> 16
        else -> 0
    }
    val starCount = if (isDay) {
        0
    } else {
        when (family) {
            DecorFamily.CLEAR -> 5
            DecorFamily.PARTLY_CLOUDY -> 3
            DecorFamily.CLOUDY -> 2
            else -> 0
        }
    }
    val celestial = when (family) {
        DecorFamily.CLEAR, DecorFamily.PARTLY_CLOUDY -> Offset(0.78f, 0.18f)
        else -> null
    }
    val streaks = List(streakCount) { i ->
        val col = i % 6
        val row = i / 6
        Offset(x = 0.12f + col * 0.15f, y = 0.58f + row * 0.14f)
    }
    return DecorLayout(
        celestial = celestial,
        stars = starSlots.take(starCount),
        clouds = cloudSlots.take(cloudCount),
        streaks = streaks
    )
}
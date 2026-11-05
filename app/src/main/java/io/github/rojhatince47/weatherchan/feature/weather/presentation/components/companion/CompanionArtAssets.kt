package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose

/**
 * Gerçek çizimler geldikçe doldurulacak harita. Boş pozlar Canvas yer
 * tutucusuyla çizilir; böylece görseller parça parça devreye alınabilir.
 */
internal object CompanionArtAssets {
    val overrides: Map<CompanionPose, Int> = mapOf(
        CompanionPose.CHEERFUL to R.drawable.companion_cheerful,
        CompanionPose.RAINY to R.drawable.companion_rainy,
        CompanionPose.COLD to R.drawable.companion_cold,
        CompanionPose.WINDY to R.drawable.companion_windy,
        CompanionPose.CALM to R.drawable.companion_calm,
        CompanionPose.SLEEPY to R.drawable.companion_sleepy,
        CompanionPose.SURPRISED to R.drawable.companion_surprised,
        CompanionPose.TAP_GIGGLE to R.drawable.companion_tap_giggle,
        CompanionPose.TAP_LAUGH to R.drawable.companion_tap_laugh,
        CompanionPose.TAP_HUM to R.drawable.companion_tap_hum,
        CompanionPose.TAP_SIGH to R.drawable.companion_tap_sigh
    )
}

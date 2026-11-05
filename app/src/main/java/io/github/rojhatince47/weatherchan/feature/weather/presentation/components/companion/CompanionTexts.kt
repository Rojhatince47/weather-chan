package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceCategory
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose

/** Kategori → replik dizisi kaynağı (spec §3.3). */
@StringRes
internal fun AdviceCategory.textArrayRes(): Int = when (this) {
    AdviceCategory.STORM -> R.array.advice_storm
    AdviceCategory.SNOW -> R.array.advice_snow
    AdviceCategory.RAIN -> R.array.advice_rain
    AdviceCategory.RAIN_SOON -> R.array.advice_rain_soon
    AdviceCategory.WINDY -> R.array.advice_windy
    AdviceCategory.COLD -> R.array.advice_cold
    AdviceCategory.HOT -> R.array.advice_hot
    AdviceCategory.NIGHT -> R.array.advice_night
    AdviceCategory.CLEAR -> R.array.advice_clear
    AdviceCategory.CLOUDY -> R.array.advice_cloudy
}

/** Poz → erişilebilirlik sıfatı. */
@StringRes
internal fun CompanionPose.labelRes(): Int = when (this) {
    CompanionPose.CHEERFUL -> R.string.companion_pose_cheerful
    CompanionPose.RAINY -> R.string.companion_pose_rainy
    CompanionPose.COLD -> R.string.companion_pose_cold
    CompanionPose.WINDY -> R.string.companion_pose_windy
    CompanionPose.CALM -> R.string.companion_pose_calm
    CompanionPose.SLEEPY -> R.string.companion_pose_sleepy
    CompanionPose.SURPRISED -> R.string.companion_pose_surprised
    CompanionPose.TAP_GIGGLE -> R.string.companion_pose_tap_giggle
    CompanionPose.TAP_LAUGH -> R.string.companion_pose_tap_laugh
    CompanionPose.TAP_HUM -> R.string.companion_pose_tap_hum
    CompanionPose.TAP_SIGH -> R.string.companion_pose_tap_sigh
}

/**
 * Deterministik satır seçimi. Negatif tohumlarda da daima `0 until size`
 * aralığına düşer; dizi boyu diller arasında farklı olabilir.
 */
internal fun lineIndex(size: Int, seed: Int): Int = Math.floorMod(seed, size)

@Composable
internal fun adviceLine(category: AdviceCategory, seed: Int): String {
    val lines = stringArrayResource(category.textArrayRes())
    return lines[lineIndex(lines.size, seed)]
}

@Composable
internal fun tapLine(vibe: CompanionTextVibe, seed: Int, tapCount: Int): String {
    val lines = stringArrayResource(vibe.arrayRes)
    return lines[lineIndex(lines.size, seed + tapCount)]
}

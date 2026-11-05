package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import androidx.annotation.ArrayRes
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose

/** Dokunuş tepkisinin metin havuzu. */
internal enum class CompanionTextVibe(@ArrayRes val arrayRes: Int) {
    SURPRISED(R.array.companion_tap_surprised),
    PLAYFUL(R.array.companion_tap_playful),
    SOFT(R.array.companion_tap_soft)
}

/**
 * Dokunuşta birlikte seçilen görsel + ses + metin paketi. Üçü ayrı ayrı
 * dönmediği için poz, ses ve replik birbirini tutar.
 */
internal data class CompanionReaction(
    val pose: CompanionPose,
    val clipIndex: Int,
    val textVibe: CompanionTextVibe
)

/**
 * Sıra sabittir; her dokunuşta bir sonraki paket seçilir. `TAP_SNICKER`
 * görseli henüz olmadığı için listede yok; görsel gelince eklenecek.
 */
internal val companionReactions: List<CompanionReaction> = listOf(
    CompanionReaction(CompanionPose.SURPRISED, clipIndex = 3, textVibe = CompanionTextVibe.SURPRISED),
    CompanionReaction(CompanionPose.TAP_GIGGLE, clipIndex = 0, textVibe = CompanionTextVibe.PLAYFUL),
    CompanionReaction(CompanionPose.TAP_HUM, clipIndex = 2, textVibe = CompanionTextVibe.SOFT),
    CompanionReaction(CompanionPose.TAP_LAUGH, clipIndex = 1, textVibe = CompanionTextVibe.PLAYFUL)
    // Rahatlamış gülüş (3.12 görseli) gelince 5+6 birleşik sesle eklenecek.
)

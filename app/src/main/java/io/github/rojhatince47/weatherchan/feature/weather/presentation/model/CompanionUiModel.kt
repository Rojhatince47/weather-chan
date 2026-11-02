package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceCategory
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose

/**
 * Weather-chan kartının çizim için ihtiyaç duyduğu tek model.
 *
 * Tavsiye metni burada tutulmaz; kategori + tohum ile composable kaynaktan
 * deterministik satırı seçer. [variantSeed] negatif olabilir; seçim
 * `Math.floorMod` ile normalize edilir.
 */
data class CompanionUiModel(
    val pose: CompanionPose,
    val category: AdviceCategory,
    val variantSeed: Int
)

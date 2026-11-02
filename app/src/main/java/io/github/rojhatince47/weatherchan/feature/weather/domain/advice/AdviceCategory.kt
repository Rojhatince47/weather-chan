package io.github.rojhatince47.weatherchan.feature.weather.domain.advice

/**
 * Tavsiye kategorisi. Öncelik sırası güvenlikten konfora doğrudur:
 * STORM > SNOW > RAIN > RAIN_SOON > WINDY > COLD > HOT > NIGHT > CLEAR > CLOUDY.
 */
enum class AdviceCategory {
    STORM,
    SNOW,
    RAIN,
    RAIN_SOON,
    WINDY,
    COLD,
    HOT,
    NIGHT,
    CLEAR,
    CLOUDY
}

/** Kategori → poz eşlemesi (spec §3.3 tablosu). */
fun AdviceCategory.pose(): CompanionPose = when (this) {
    AdviceCategory.STORM -> CompanionPose.RAINY
    AdviceCategory.SNOW -> CompanionPose.COLD
    AdviceCategory.RAIN -> CompanionPose.RAINY
    AdviceCategory.RAIN_SOON -> CompanionPose.RAINY
    AdviceCategory.WINDY -> CompanionPose.WINDY
    AdviceCategory.COLD -> CompanionPose.COLD
    AdviceCategory.HOT -> CompanionPose.CHEERFUL
    AdviceCategory.NIGHT -> CompanionPose.SLEEPY
    AdviceCategory.CLEAR -> CompanionPose.CHEERFUL
    AdviceCategory.CLOUDY -> CompanionPose.CALM
}

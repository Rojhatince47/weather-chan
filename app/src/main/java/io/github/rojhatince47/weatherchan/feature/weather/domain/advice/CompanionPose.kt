package io.github.rojhatince47.weatherchan.feature.weather.domain.advice

/**
 * Weather-chan'ın kartta takındığı poz. Altı hava pozu, bir şaşkın tepkisi
 * ve dört dokunuş tepkisi; kategori eşlemesi [AdviceCategory.pose] ile
 * yapılır.
 */
enum class CompanionPose {
    CHEERFUL,
    RAINY,
    COLD,
    WINDY,
    CALM,
    SLEEPY,
    SURPRISED,
    TAP_GIGGLE,
    TAP_LAUGH,
    TAP_HUM,
    TAP_SIGH
}

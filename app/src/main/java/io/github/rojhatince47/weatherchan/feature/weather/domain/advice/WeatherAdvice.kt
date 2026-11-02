package io.github.rojhatince47.weatherchan.feature.weather.domain.advice

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition

/** Bir saatlik dilimin tavsiye motoruna görünüşü. */
data class HourlyOutlook(
    val precipitationProbabilityPercent: Int,
    val condition: WeatherCondition
)

/** Tavsiye motorunun saf girdisi; Android/Compose bağımlılığı yoktur. */
data class AdviceContext(
    val cityId: Long,
    val temperatureCelsius: Int,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val windSpeedKmh: Int,
    val upcomingHours: List<HourlyOutlook>,
    val epochDay: Long,
    val hourOfDay: Int
)

/** Motorun kararı: kategori, poz ve tekrar çizimde değişmeyen tohum. */
data class WeatherAdvice(
    val category: AdviceCategory,
    val pose: CompanionPose,
    val variantSeed: Int
)

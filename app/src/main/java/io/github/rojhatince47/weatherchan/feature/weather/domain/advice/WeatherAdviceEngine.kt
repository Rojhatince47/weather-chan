package io.github.rojhatince47.weatherchan.feature.weather.domain.advice

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import javax.inject.Inject

/**
 * Kural tabanlı, tamamen çevrimdışı tavsiye motoru.
 *
 * Kategori önceliği spec §3.2 tablosudur; eşikler tek bir companion object'te
 * adlandırılmıştır. Tohum (şehir, gün, saat, kategori) dörtlüsünden üretilir;
 * aynı girdi aynı repliği seçer, negatif tohum UI'da `floorMod` ile normalize
 * edilir.
 */
class WeatherAdviceEngine @Inject constructor() {

    fun advise(context: AdviceContext): WeatherAdvice {
        val category = categoryOf(context)
        return WeatherAdvice(
            category = category,
            pose = category.pose(),
            variantSeed = seedOf(context, category)
        )
    }

    private fun categoryOf(context: AdviceContext): AdviceCategory = when {
        context.condition == WeatherCondition.THUNDERSTORM -> AdviceCategory.STORM
        context.condition == WeatherCondition.SNOW -> AdviceCategory.SNOW
        context.condition == WeatherCondition.RAIN ||
            context.condition == WeatherCondition.LIGHT_SHOWERS -> AdviceCategory.RAIN
        context.upcomingHours.any { hour ->
            (hour.condition == WeatherCondition.RAIN ||
                hour.condition == WeatherCondition.LIGHT_SHOWERS) &&
                hour.precipitationProbabilityPercent >= RAIN_SOON_THRESHOLD
        } -> AdviceCategory.RAIN_SOON
        context.windSpeedKmh >= WINDY_KMH -> AdviceCategory.WINDY
        context.temperatureCelsius <= COLD_MAX_C -> AdviceCategory.COLD
        context.temperatureCelsius >= HOT_MIN_C -> AdviceCategory.HOT
        !context.isDay -> AdviceCategory.NIGHT
        context.condition == WeatherCondition.CLOUDY -> AdviceCategory.CLOUDY
        else -> AdviceCategory.CLEAR
    }

    private fun seedOf(context: AdviceContext, category: AdviceCategory): Int {
        var hash = 17
        hash = 31 * hash + context.cityId.hashCode()
        hash = 31 * hash + context.epochDay.hashCode()
        hash = 31 * hash + context.hourOfDay
        hash = 31 * hash + category.ordinal
        return hash
    }

    private companion object {
        const val RAIN_SOON_THRESHOLD = 50
        const val WINDY_KMH = 35
        const val COLD_MAX_C = 10
        const val HOT_MIN_C = 30
    }
}

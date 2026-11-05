package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition

/** Hava durumuna göre kısa çevre sesi. */
internal enum class CompanionAmbientClip {
    BIRDS,
    CRICKETS,
    WIND,
    RAIN,
    SNOW,
    STORM
}

/**
 * Hava koşulunu çevre sesine eşler. Saf fonksiyon: Android'e bağlı değildir,
 * birim testiyle doğrulanır.
 */
internal fun ambientClipFor(condition: WeatherCondition, isDay: Boolean): CompanionAmbientClip = when (condition) {
    WeatherCondition.CLEAR -> if (isDay) CompanionAmbientClip.BIRDS else CompanionAmbientClip.CRICKETS
    WeatherCondition.PARTLY_CLOUDY, WeatherCondition.CLOUDY -> CompanionAmbientClip.WIND
    WeatherCondition.RAIN, WeatherCondition.LIGHT_SHOWERS -> CompanionAmbientClip.RAIN
    WeatherCondition.SNOW -> CompanionAmbientClip.SNOW
    WeatherCondition.THUNDERSTORM -> CompanionAmbientClip.STORM
}

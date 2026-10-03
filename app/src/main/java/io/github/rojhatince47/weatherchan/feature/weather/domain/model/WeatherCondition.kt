package io.github.rojhatince47.weatherchan.feature.weather.domain.model

enum class WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,

    /**
     * Hafif çisenti / hafif sağanak (eser miktar): tam yağmur gibi göstermek
     * kullanıcı gözünde yanlıştır; yumuşak simge + etiket alır, paleti
     * yağmurla paylaşır.
     */
    LIGHT_SHOWERS,
    SNOW,
    THUNDERSTORM
}

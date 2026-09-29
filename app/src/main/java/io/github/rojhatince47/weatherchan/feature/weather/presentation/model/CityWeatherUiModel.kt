package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition

data class CityWeatherUiModel(
    val cityId: Long,
    val name: String,
    val region: String?,
    val country: String?,
    val weather: CardWeather
)

/**
 * Kartın hava durumu içeriği.
 *
 * Hava durumu alınamayan şehir de listede kalır; kart yüklenmiş veri yerine
 * nedeni ve "Tekrar dene" eylemini gösterir. Nullable alan yerine sealed tip
 * tercih edildi: her tüketiciyi derleme zamanında iki dalı da ele almaya
 * zorlar, `?.` zincirleriyle korunmak zorunda kalmaz.
 */
sealed interface CardWeather {

    /** Hava durumu yüklendi; kart koşul paleti ve sıcaklık gösterir. */
    data class Loaded(
        val temperatureText: String,
        val condition: WeatherCondition,
        val conditionEmoji: String,
        val conditionLabel: UiText
    ) : CardWeather

    /** Hava durumu alınamadı; kart nedeni ve eylemi gösterir. */
    data object Unavailable : CardWeather
}

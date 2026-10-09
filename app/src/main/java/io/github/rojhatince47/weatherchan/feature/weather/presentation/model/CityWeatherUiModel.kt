package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition

data class CityWeatherUiModel(
    val cityId: Long,
    val name: String,
    val region: String?,
    val country: String?,
    val weather: CardWeather,
    /** Konum kartı "Konumunuz" etiketi taşır; görüntüleme kaygısıdır. */
    val isLocation: Boolean = false
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
        val icon: WeatherIcon,
        val conditionLabel: UiText,
        /** Kartın gün bilgisi; palet seçiminde kullanılır. Default = mevcut çağrılar değişmez. */
        val isDay: Boolean = true
    ) : CardWeather

    /** Hava durumu alınamadı; kart nedeni ve eylemi gösterir. */
    data object Unavailable : CardWeather
}

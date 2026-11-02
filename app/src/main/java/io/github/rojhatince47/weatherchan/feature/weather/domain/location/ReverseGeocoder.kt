package io.github.rojhatince47.weatherchan.feature.weather.domain.location

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates

/**
 * Koordinatın yerleşim adı (geri çözümleme). Uygulama dilinde döner —
 * wttr.in zincirinin dil/yazım sorunları bu portla giderilir.
 */
data class PlaceName(
    val name: String,
    val region: String?,
    val country: String? = null,
    /** ISO-3166 ülke kodu (ör. "TR"); geri çözümleyici biliyorsa doldurur. */
    val countryCode: String? = null
)

/**
 * Geri çözümleme portu. Implementasyonlar **hata fırlatmaz**: çözülemezse
 * `null` döner (konum eksikliği veri eksikliğidir, hata değildir).
 */
interface ReverseGeocoder {
    suspend fun reverse(coordinates: Coordinates): PlaceName?
}

package io.github.rojhatince47.weatherchan.feature.city.domain.suggestion

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City

/** Tek bir öneri. [distanceKm] yalnız "Yakınında" dalında doludur. */
data class CitySuggestion(
    val city: City,
    val distanceKm: Int?
)

/**
 * Öneri dalı. UI başlığı burada değil kaynakta tutulur: domain kaynak
 * (`R.string`) bilemez, ekran `SuggestionKind`'e göre metin seçer.
 */
enum class SuggestionKind {
    /** Konum biliniyor: mesafeye göre sıralı. */
    Nearby,

    /** Konum yok, cihaz ülkesi biliniyor: dosya sırası. */
    Country,

    /** Ne konum ne ülke biliniyor: dünya yedeği. */
    World
}

/** Ekranda aynı anda tek grup görünür; öncelik zinciri use case'te biter. */
data class SuggestionGroup(
    val kind: SuggestionKind,
    val suggestions: List<CitySuggestion>
)

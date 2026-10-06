package io.github.rojhatince47.weatherchan.feature.city.domain.suggestion

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates

interface NearbyCitySuggestionProvider {
    /** Önbellekli okuma; canlı sorgu yalnız önbellek taze değilse yapılır. */
    suspend fun nearby(origin: Coordinates): List<CitySeed>?

    /** Canlı sorguyu zorlar; başarılıysa önbelleğe yazar. */
    suspend fun refresh(origin: Coordinates): List<CitySeed>?
}

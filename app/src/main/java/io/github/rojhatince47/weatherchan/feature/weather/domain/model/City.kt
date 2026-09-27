package io.github.rojhatince47.weatherchan.feature.weather.domain.model

import kotlin.math.roundToLong

data class City(
    val name: String,
    val region: String?,
    val country: String?,
    val coordinates: Coordinates,
    val isFavorite: Boolean = false
) {
    /**
     * Koordinattan deterministik kimlik.
     *
     * Geocoding'in kendi id'si gömülü listede yoktur; aynı şehir iki kaynaktan
     * geldiğinde iki farklı id alırsa mükerrer ekleme ve "olmayan şehir favorisi"
     * durumları doğar. 3 basamak ~110 m çözünürlük verir. Çarpan 10_000_000,
     * boylam×1000'ün en büyük değerini (180 000) aştığı için iki bileşen
     * birbirine karışmaz.
     */
    val id: Long
        get() = (coordinates.latitude * 1_000).roundToLong() * 10_000_000 +
            (coordinates.longitude * 1_000).roundToLong()
}

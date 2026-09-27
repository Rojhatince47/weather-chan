package io.github.rojhatince47.weatherchan.feature.city.domain.repository

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import kotlinx.coroutines.flow.StateFlow

/**
 * Kullanıcının şehir listesinin tek gerçek kaynağı.
 *
 * Favoriler ayrı bir depo değildir; [City.isFavorite] bir alandır. Bu yüzden
 * "listede olmayan şehir favorisi" gibi tutarsız bir durum oluşamaz.
 */
interface CityRepository {
    /** Her zaman favoriler en üstte sıralı döner. */
    val cities: StateFlow<List<City>>

    /** Zaten listede olan şehir eklenirse mevcut kayıt korunur (yıldızı kaybolmaz). */
    fun add(city: City)

    fun remove(cityId: Long)

    fun toggleFavorite(cityId: Long)
}

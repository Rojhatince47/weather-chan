package io.github.rojhatince47.weatherchan.feature.city.domain.model

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City

/**
 * Favorileri listenin en üstüne alan tek sıralama kuralı.
 *
 * `sortedByDescending` kararlı (stable) olduğu için favorilerin kendi aralarındaki
 * sırası ve favori olmayanların sırası korunur; kararsız bir sıralama kullanıcının
 * listesinin her yenilemede zıplamasına yol açardı.
 */
object CitySort {
    fun favoritesFirst(cities: List<City>): List<City> = cities.sortedByDescending { it.isFavorite }
}

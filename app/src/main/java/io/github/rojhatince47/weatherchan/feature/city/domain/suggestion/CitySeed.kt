package io.github.rojhatince47.weatherchan.feature.city.domain.suggestion

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates

/**
 * Gömülü öneri listesindeki tek bir şehir kaydı.
 *
 * `region` **yoktur**: geocoding kaydına bölge yazmak bu boyda bir liste için
 * ek risk getirir, kazancı yalnız kartın alt satırında bir kelime olur.
 */
data class CitySeed(
    val name: String,
    val countryCode: String,
    val countryName: String,
    val coordinates: Coordinates
) {
    fun toCity(): City = City(
        name = name,
        region = null,
        country = countryName,
        coordinates = coordinates
    )
}

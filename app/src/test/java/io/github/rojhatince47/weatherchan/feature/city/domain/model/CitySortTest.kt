package io.github.rojhatince47.weatherchan.feature.city.domain.model

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Test

class CitySortTest {

    private fun city(name: String, favorite: Boolean) = City(
        name = name,
        region = null,
        country = "Türkiye",
        coordinates = Coordinates(latitude = 40.0 + name.length / 10.0, longitude = 29.0),
        isFavorite = favorite
    )

    @Test
    fun `favoriler en ustte olur`() {
        val sorted = CitySort.favoritesFirst(
            listOf(
                city("Ankara", favorite = false),
                city("Izmir", favorite = true),
                city("Bursa", favorite = false),
                city("Adana", favorite = true)
            )
        )
        assertEquals(listOf("Izmir", "Adana", "Ankara", "Bursa"), sorted.map { it.name })
    }

    @Test
    fun `favori yoksa liste siralamasi korunur`() {
        val input = listOf(city("Ankara", false), city("Bursa", false), city("Izmir", false))
        assertEquals(input, CitySort.favoritesFirst(input))
    }

    @Test
    fun `favorilerin birbirindeki sirasi degismez`() {
        val sorted = CitySort.favoritesFirst(
            listOf(
                city("C", favorite = true),
                city("A", favorite = true),
                city("B", favorite = true)
            )
        )
        assertEquals(listOf("C", "A", "B"), sorted.map { it.name })
    }

    @Test
    fun `bos liste bos kalir`() {
        assertEquals(emptyList<City>(), CitySort.favoritesFirst(emptyList()))
    }
}

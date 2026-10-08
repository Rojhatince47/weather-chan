package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CardWeather
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionListsTest {

    private val cityA = City("A", "B", "C", Coordinates(1.0, 1.0))

    @Test
    fun `varsayilanlar bos ve null`() {
        val r = SearchResultUiModel(cityA, isInList = false)
        assertEquals(null, r.distanceKm)
        assertEquals(null, r.cardWeather)
    }

    @Test
    fun `cardWeather tasima`() {
        val r = SearchResultUiModel(cityA, isInList = false, cardWeather = CardWeather.Unavailable)
        assertEquals(CardWeather.Unavailable, r.cardWeather)
    }
}

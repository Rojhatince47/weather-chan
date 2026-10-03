package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Test

class PagerRulesTest {
    private fun city(name: String, isLocation: Boolean = false) =
        City(name = name, region = "", country = "", coordinates = Coordinates(0.0, 0.0), isLocation = isLocation)

    @Test
    fun buildPages_placesLocationFirst_ifPresent() {
        val pages = buildPages(listOf(city("A"), city("Konum", isLocation = true), city("B")))
        assertEquals(3, pages.size)
        assertEquals(true, pages[0].isLocation)
        assertEquals("A", pages[1].name)
        assertEquals("B", pages[2].name)
    }

    @Test
    fun buildPages_doesNotDuplicateLocation() {
        val pages = buildPages(listOf(city("Konum", isLocation = true), city("A")))
        assertEquals(2, pages.size)
        assertEquals(1, pages.count { it.isLocation })
    }

    @Test
    fun buildPages_keepsOriginalOrderForOthers() {
        val pages = buildPages(listOf(city("X"), city("Y"), city("Z")))
        assertEquals("X", pages[0].name)
        assertEquals("Y", pages[1].name)
        assertEquals("Z", pages[2].name)
    }

    @Test
    fun buildPages_handlesEmpty() {
        val pages = buildPages(emptyList())
        assertEquals(0, pages.size)
    }

    @Test
    fun clampPage_negative_returns0() {
        assertEquals(0, clampPage(-1, 3))
    }

    @Test
    fun clampPage_largerThanLast_returnsLast() {
        assertEquals(2, clampPage(5, 3))
    }

    @Test
    fun clampPage_valid_returnsItself() {
        assertEquals(1, clampPage(1, 3))
    }

    @Test
    fun clampPage_emptySize_returns0() {
        assertEquals(0, clampPage(10, 0))
    }
}

package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City

internal fun buildPages(stored: List<City>): List<City> {
    if (stored.isEmpty()) return emptyList()
    val location = stored.find { it.isLocation }
    val others = stored.filter { !it.isLocation }
    return if (location != null) {
        listOf(location) + others
    } else {
        others
    }
}

internal fun clampPage(index: Int, size: Int): Int = when {
    size <= 0 -> 0
    index <= 0 -> 0
    index >= size - 1 -> size - 1
    else -> index
}

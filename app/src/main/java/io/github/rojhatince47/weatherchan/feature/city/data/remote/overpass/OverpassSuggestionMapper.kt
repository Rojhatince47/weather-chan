package io.github.rojhatince47.weatherchan.feature.city.data.remote.overpass

import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySeed
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates

internal class OverpassSuggestionMapper {
    fun toSeeds(elements: List<OverpassElementDto>, preferEnglish: Boolean): List<CitySeed> =
        elements.mapNotNull { element ->
            val local = element.tags["name"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = if (preferEnglish) {
                element.tags["name:en"]?.takeIf { it.isNotBlank() } ?: local
            } else {
                local
            }
            CitySeed(
                name = name,
                countryCode = "",
                countryName = "",
                coordinates = Coordinates(element.lat, element.lon)
            )
        }
}

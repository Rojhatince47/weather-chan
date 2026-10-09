package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

/**
 * Koşul simgesi üretimi tek noktadan ([WeatherUiMapper.iconFor]) yapılır.
 * Unicode'ta "parçalı bulutlu ay" emojisi olmadığından o durum çizimdir;
 * kalanlar tek kod noktalı gliftir (U+FE0F içermez, iki karakter görünmez).
 */
sealed interface WeatherIcon {

    /** Tek kod noktalı emoji sembolü. */
    data class Glyph(val value: String) : WeatherIcon

    /** "Parçalı bulutlu ay" — hilal bulut arkasında, iki tonlu çizim. */
    data object MoonBehindCloud : WeatherIcon
}

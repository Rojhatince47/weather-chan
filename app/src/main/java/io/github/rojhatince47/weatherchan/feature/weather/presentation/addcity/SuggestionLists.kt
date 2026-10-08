package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

/**
 * Sunum düzeyinde öneri grupları — domain `SuggestionGroup` adıyla çakışmaz.
 * Bucket sırası nearby → country → world.
 */
data class SuggestionLists(
    val nearby: List<SearchResultUiModel> = emptyList(),
    val country: List<SearchResultUiModel> = emptyList(),
    val world: List<SearchResultUiModel> = emptyList()
)

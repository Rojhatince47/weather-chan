package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

data class AddCityUiState(
    val query: String = "",
    val suggestions: SuggestionLists? = null,
    val searchState: SearchUiState = SearchUiState.Idle
)

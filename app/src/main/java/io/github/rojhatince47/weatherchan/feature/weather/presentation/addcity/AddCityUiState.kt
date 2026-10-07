package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionGroup

data class AddCityUiState(
    val query: String = "",
    /** Sorgu boşken görünür. `null` → öneri yok, bölüm çizilmez. */
    val suggestions: SuggestionGroup? = null,
    /** Depodaki güncel kimlikler: satır "ekli mi" bayrağı (UI tik için kullanır). */
    val existingIds: Set<Long> = emptySet(),
    val searchState: SearchUiState = SearchUiState.Idle
)

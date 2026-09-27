package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City

/**
 * Canlı arama ana listenin durumundan ayrı tutulur; biri yüklenirken
 * diğeri çalışmaya devam eder. Başarı boş liste olabilir, ayrı bir
 * Empty durumu yoktur (YAGNI). Sorgu 3 karakterin altına düşerse Idle.
 */
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<SearchResultUiModel>) : SearchUiState
    data class Failure(val error: AppError) : SearchUiState
}

/** Canlı sonuç satırı. Şehir zaten listede ise ekleme yerine onay işareti gösterilir. */
data class SearchResultUiModel(
    val city: City,
    val isInList: Boolean
)

package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CardWeather

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

/**
 * Arama/öneri satırı. [cardWeather] null = henüz hava durumu çekilmedi
 * (kart yükleniyor göstergesi çizer). [distanceKm] yalnız "Yakınında"
 * önerilerinde doludur.
 */
data class SearchResultUiModel(
    val city: City,
    val isInList: Boolean,
    val distanceKm: Int? = null,
    val cardWeather: CardWeather? = null
)

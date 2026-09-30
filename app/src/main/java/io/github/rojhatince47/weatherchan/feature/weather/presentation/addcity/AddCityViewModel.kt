package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestCitiesUseCase
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionGroup
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.SearchCitiesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Şehir ekleme ekranının durumu.
 *
 * Canlı arama mantığı `MainViewModel`'dan **taşındı**: ana ekranda sorgu hem
 * arama hem liste süzgeciydi, iki kaynaklı bir durumdu. Artık aramanın tek
 * sahibi bu ekran.
 */
@HiltViewModel
class AddCityViewModel @Inject constructor(
    private val searchCities: SearchCitiesUseCase,
    private val suggestCities: SuggestCitiesUseCase,
    private val cityRepository: CityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCityUiState())
    val uiState: StateFlow<AddCityUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Öneri grubu **bir kez** hesaplanır; sonraki depo yayımlarında yalnızca
     * "listede olanlar" süzülür.
     *
     * Her yayımda yeniden hesaplamak konumu (GPS okuması) tekrar tekrar
     * okuturdu; ekranda hiçbir şey değişmemişken yapılan bir işlemdir.
     */
    private var baseSuggestions: SuggestionGroup? = null

    /** Depodaki güncel kimlikler; canlı arama sonuçlarının "listede mi" bayrağı için. */
    private var existingIds: Set<Long> = emptySet()

    init {
        viewModelScope.launch {
            var computed = false
            cityRepository.cities.collect { stored ->
                existingIds = stored.mapTo(mutableSetOf()) { it.id }
                if (!computed) {
                    computed = true
                    baseSuggestions = suggestCities(stored)
                }
                // Eklenen şehir arama sonuçlarında da "listede" bayrağını
                // alır; bayrak bayat kalırsa kullanıcı aynı şehri iki kez
                // eklemeye çalışır (`MainViewModel`'dan taşınan davranış).
                refreshSearchInListFlags()
                // Grup tümüyle elendiyse bölümü boş çizmek yerine hiç
                // çizme: use case boş grup döndürmez, bu normalleştirme
                // aynı sözleşmeyi sonraki depo yayımlarında korur.
                _uiState.update {
                    it.copy(
                        suggestions = baseSuggestions?.excluding(existingIds)
                            ?.takeIf { group -> group.suggestions.isNotEmpty() }
                    )
                }
            }
        }
    }

    private fun refreshSearchInListFlags() {
        val state = _uiState.value.searchState
        if (state is SearchUiState.Success) {
            _uiState.update {
                it.copy(
                    searchState = state.copy(
                        results = state.results.map { result ->
                            result.copy(isInList = result.city.id in existingIds)
                        }
                    )
                )
            }
        }
    }

    fun onQueryChange(query: String) {
        // Sorgu bir kullanıcı girdisidir: yazılan karakter anında görünür,
        // ağ turu ise debounce sonrasında başlar.
        _uiState.update { it.copy(query = query) }
        searchLive(query)
    }

    fun onAddCity(city: City) {
        // Yazma kasıtlı olarak veri çekmez: `CityRepository.cities` yayımı
        // öneriyi zaten tazeler, ayrıca `fetch` çağırmak her eklemede iki ağ
        // turu demek olurdu.
        viewModelScope.launch { cityRepository.add(city) }
    }

    /** 3 karakterin altındaki sorgular ağa gitmez; bekleyen istek iptal edilir. */
    private fun searchLive(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < MIN_SEARCH_QUERY_LENGTH) {
            _uiState.update { it.copy(searchState = SearchUiState.Idle) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(searchState = SearchUiState.Loading) }
            when (val result = searchCities(trimmed)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        searchState = SearchUiState.Success(
                            result.data.map { city -> SearchResultUiModel(city, city.id in existingIds) }
                        )
                    )
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(searchState = SearchUiState.Failure(result.error))
                }
            }
        }
    }

    companion object {
        const val MIN_SEARCH_QUERY_LENGTH = 3
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}

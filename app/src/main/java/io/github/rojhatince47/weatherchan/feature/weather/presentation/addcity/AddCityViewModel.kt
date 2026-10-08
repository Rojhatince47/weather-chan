package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestCitiesUseCase
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionGroup
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionKind
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityProximity
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.SearchCitiesUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CardWeather
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
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
 *
 * Hava durumu arka planda çekilir: her sonuç (arama + öneri) için
 * [GetCityWeathersUseCase] ile paralel fetch başlatılır. Mini-cache +
 * job-cancellation ile stale-guard sağlanır.
 */
@HiltViewModel
class AddCityViewModel @Inject constructor(
    private val searchCities: SearchCitiesUseCase,
    private val suggestCities: SuggestCitiesUseCase,
    private val cityRepository: CityRepository,
    private val getCityWeathers: GetCityWeathersUseCase,
    private val unitRepository: TemperatureUnitRepository,
    private val weatherUiMapper: WeatherUiMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCityUiState())
    val uiState: StateFlow<AddCityUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Öneri grubu **bir kez** hesaplanır; yayım anında depo listesiyle
     * süzülür (eklenen şehir öneride kalmaz, `CityProximity`).
     */
    private var baseSuggestions: SuggestionGroup? = null

    /** Depodaki güncel liste; satır "ekli mi" kararı yakınlıkla bunun üzerinden verilir. */
    private var existingCities: List<City> = emptyList()

    private val weatherJobs = mutableListOf<Job>()
    private val weatherCache = mutableMapOf<Long, CityWeather>()

    init {
        viewModelScope.launch {
            var computed = false
            cityRepository.cities.collect { stored ->
                existingCities = stored
                if (!computed) {
                    computed = true
                    viewModelScope.launch { computeSuggestions() }
                }
                refreshSearchInListFlags()
                publishSuggestions()
            }
        }
        viewModelScope.launch {
            unitRepository.unit.collect { remapVisibleWeather() }
        }
    }

    /**
     * Aşama A → B → C, **bir kez** çalışır. Ayrı coroutine'dedir: depo
     * toplayıcısı konum (12 sn GPS) ve Overpass ağ turlarını beklemez.
     */
    private suspend fun computeSuggestions() {
        baseSuggestions = suggestCities(nearbyEnabled = false)
        publishSuggestions()
        val withNearby = suggestCities()
        if (withNearby != null && withNearby != baseSuggestions) {
            baseSuggestions = withNearby
            publishSuggestions()
        }
        if (baseSuggestions?.kind == SuggestionKind.Nearby || baseSuggestions == null) {
            val fresh = suggestCities(refresh = true)
            if (fresh != null && fresh != baseSuggestions) {
                baseSuggestions = fresh
                publishSuggestions()
            }
        }
    }

    /**
     * Grup, depodaki güncel listeyle süzülerek yayımlanır: eklenen şehir
     * öneride kalmaz.
     *
     * Sahip-bilgili merge: `kind` = [baseSuggestions]?.kind (filtrelenmiş
     * değil). Aktif bucket her zaman overwrite edilir (boş dahil); diğer
     * bucket'lar korunur. `baseSuggestions == null` ise tüm bucket'lar
     * korunur.
     */
    private fun publishSuggestions() {
        cancelWeatherJobs()
        val filtered = baseSuggestions
            ?.let { g ->
                g.copy(
                    suggestions = g.suggestions.filterNot { suggestion ->
                        existingCities.any { CityProximity.isSamePlace(suggestion.city, it) }
                    }
                )
            }
            ?.takeIf { g -> g.suggestions.isNotEmpty() }
        val list = filtered?.suggestions.orEmpty().map { s ->
            SearchResultUiModel(s.city, isInList = false, distanceKm = s.distanceKm)
        }
        val kind = baseSuggestions?.kind
        _uiState.update { prev ->
            val merged = SuggestionLists(
                nearby = if (kind == SuggestionKind.Nearby) list else prev.suggestions?.nearby.orEmpty(),
                country = if (kind == SuggestionKind.Country) list else prev.suggestions?.country.orEmpty(),
                world = if (kind == SuggestionKind.World) list else prev.suggestions?.world.orEmpty()
            )
            prev.copy(
                suggestions = merged.takeIf { l ->
                    l.nearby.isNotEmpty() || l.country.isNotEmpty() || l.world.isNotEmpty()
                }
            )
        }
        restartPendingFetches()
    }

    private fun refreshSearchInListFlags() {
        val state = _uiState.value.searchState
        if (state is SearchUiState.Success) {
            _uiState.update {
                it.copy(
                    searchState = state.copy(
                        results = state.results.map { result ->
                            result.copy(isInList = existingCities.any { CityProximity.isSamePlace(result.city, it) })
                        }
                    )
                )
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchLive(query)
    }

    fun onAddCity(city: City) {
        viewModelScope.launch { cityRepository.add(city) }
    }

    /** 3 karakterin altındaki sorgular ağa gitmez; bekleyen istek iptal edilir. */
    private fun searchLive(query: String) {
        searchJob?.cancel()
        cancelWeatherJobs()
        val trimmed = query.trim()
        if (trimmed.length < MIN_SEARCH_QUERY_LENGTH) {
            _uiState.update { it.copy(searchState = SearchUiState.Idle) }
            restartPendingFetches()
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(searchState = SearchUiState.Loading) }
            when (val result = searchCities(trimmed)) {
                is AppResult.Success -> {
                    val results = result.data.map { city ->
                        SearchResultUiModel(city, existingCities.any { CityProximity.isSamePlace(city, it) })
                    }
                    _uiState.update { it.copy(searchState = SearchUiState.Success(results)) }
                    results.forEach { r ->
                        if (r.cardWeather == null) {
                            weatherCache[r.city.id]?.let { c ->
                                updateResultWeatherByCityId(r.city.id, toCardWeather(c))
                            } ?: launchWeatherFetch(r.city)
                        }
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(searchState = SearchUiState.Failure(result.error))
                }
            }
        }
    }

    private fun launchWeatherFetch(city: City) {
        weatherCache[city.id]?.let { cached ->
            updateResultWeatherByCityId(city.id, toCardWeather(cached))
            return
        }
        val job = viewModelScope.launch {
            val result = getCityWeathers(listOf(city))
            val cardWeather = when (result) {
                is AppResult.Success -> result.data.firstOrNull()?.let {
                    weatherCache[city.id] = it
                    toCardWeather(it)
                } ?: CardWeather.Unavailable
                is AppResult.Failure -> CardWeather.Unavailable
            }
            updateResultWeatherByCityId(city.id, cardWeather)
        }
        weatherJobs += job
    }

    private fun toCardWeather(cityWeather: CityWeather): CardWeather =
        weatherUiMapper.toCityWeatherUiModel(cityWeather, unit = unitRepository.unit.value).weather

    private fun cancelWeatherJobs() {
        weatherJobs.forEach { it.cancel() }
        weatherJobs.clear()
    }

    /** Mini-cache'ten yeniden haritalama (ağ yok) — birim değişince çağrılır. */
    private fun remapVisibleWeather() {
        val unit = unitRepository.unit.value
        weatherCache.forEach { (id, cw) ->
            updateResultWeatherByCityId(id, weatherUiMapper.toCityWeatherUiModel(cw, unit = unit).weather)
        }
    }

    /**
     * Benzersiz şehir id'leri için fetch başlat (cross-bucket dedup).
     * Hem öneri hem arama satırlarını kapsar.
     */
    private fun restartPendingFetches() {
        val visibleIds = mutableSetOf<Long>()
        (_uiState.value.suggestions?.let { it.nearby + it.country + it.world } ?: emptyList())
            .plus((_uiState.value.searchState as? SearchUiState.Success)?.results.orEmpty())
            .forEach { result ->
                if (result.city.id !in visibleIds) {
                    visibleIds.add(result.city.id)
                    if (result.cardWeather == null) {
                        weatherCache[result.city.id]?.let { c ->
                            updateResultWeatherByCityId(result.city.id, toCardWeather(c))
                        } ?: launchWeatherFetch(result.city)
                    }
                }
            }
    }

    private fun updateResultWeatherByCityId(id: Long, cardWeather: CardWeather) {
        _uiState.update { state ->
            val updatedSearch = (state.searchState as? SearchUiState.Success)?.let { s ->
                s.copy(results = s.results.map { if (it.city.id == id) it.copy(cardWeather = cardWeather) else it })
            } ?: state.searchState
            val updatedSuggestions = state.suggestions?.let { lists ->
                SuggestionLists(
                    nearby = lists.nearby.map { if (it.city.id == id) it.copy(cardWeather = cardWeather) else it },
                    country = lists.country.map { if (it.city.id == id) it.copy(cardWeather = cardWeather) else it },
                    world = lists.world.map { if (it.city.id == id) it.copy(cardWeather = cardWeather) else it }
                )
            }
            state.copy(searchState = updatedSearch, suggestions = updatedSuggestions)
        }
    }

    companion object {
        const val MIN_SEARCH_QUERY_LENGTH = 3
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}

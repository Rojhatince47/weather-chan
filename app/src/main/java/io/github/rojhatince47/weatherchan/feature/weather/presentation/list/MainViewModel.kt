package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.state.LoadState
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.state.toLoadState
import io.github.rojhatince47.weatherchan.core.ui.text.toUiText
import io.github.rojhatince47.weatherchan.feature.favorites.domain.repository.FavoriteRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getCityWeathers: GetCityWeathersUseCase,
    private val uiMapper: WeatherUiMapper,
    private val unitRepository: TemperatureUnitRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val loadedCities = mutableMapOf<Long, City>()
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            unitRepository.unit.collect {
                refresh()
            }
        }
        viewModelScope.launch {
            favoriteRepository.favoriteIds.collect {
                refresh()
            }
        }
    }

    fun findCity(cityId: Long): City? = loadedCities[cityId]

    fun onQueryChange(query: String) {
        refresh(query = query)
    }

    fun onFavoritesOnlyChange(showFavoritesOnly: Boolean) {
        refresh(showFavoritesOnly = showFavoritesOnly)
    }

    fun onRetry() {
        loadData()
    }

    fun onRefresh() {
        loadData()
    }

    fun onFavoriteClick(cityId: Long) {
        if (cityId !in loadedCities) return
        favoriteRepository.toggleFavorite(cityId)
    }

    fun loadData() {
        // Önceki yükleme varsa iptal edilir; yeniden deneme her zaman
        // yeni bir çağrı başlatır ve eşzamanlı yinelenen istek oluşmaz.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(content = UiState.Loading) }
            _loadState.update { LoadState.Loading }
            try {
                filter(_uiState.value.query)
            } catch (e: CancellationException) {
                // İptal bir hata değildir; hata durumu olarak sunulmaz.
                throw e
            }
        }
    }

    private fun refresh(query: String = _uiState.value.query, showFavoritesOnly: Boolean = _uiState.value.showFavoritesOnly) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                filter(query, showFavoritesOnly)
            } catch (e: CancellationException) {
                // İptal bir hata değildir; hata durumu olarak sunulmaz.
                throw e
            }
        }
    }

    private suspend fun filter(query: String = _uiState.value.query, showFavoritesOnly: Boolean = _uiState.value.showFavoritesOnly) {
        val result = getCityWeathers()
        if (result is AppResult.Failure) {
            _uiState.update {
                it.copy(
                    query = query,
                    showFavoritesOnly = showFavoritesOnly,
                    content = UiState.Error(result.error.toUiText())
                )
            }
            _loadState.update { _uiState.value.content.toLoadState() }
            return
        }
        val cityWeathers = (result as AppResult.Success).data
        loadedCities.clear()
        loadedCities.putAll(cityWeathers.associate { it.city.id to it.city })

        // Sonuç ayrı bir state olarak saklanmaz; her seferinde mevcut
        // seçimlerden (sorgu + favori filtresi) türetilir.
        val favorites = favoriteRepository.favoriteIds.value
        val filtered = cityWeathers
            .filter { cityWeather ->
                query.isBlank() ||
                    cityWeather.city.name.contains(query, ignoreCase = true)
            }
            .filter { cityWeather ->
                !showFavoritesOnly || cityWeather.city.id in favorites
            }

        _uiState.update {
            it.copy(
                query = query,
                showFavoritesOnly = showFavoritesOnly,
                content = if (filtered.isEmpty()) {
                    UiState.Empty
                } else {
                    UiState.Success(
                        filtered.map { cityWeather ->
                            uiMapper.toCityWeatherUiModel(
                                cityWeather,
                                isFavorite = cityWeather.city.id in favoriteRepository.favoriteIds.value,
                                unit = unitRepository.unit.value
                            )
                        }
                    )
                }
            )
        }
        _loadState.update { _uiState.value.content.toLoadState() }
    }
}

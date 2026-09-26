package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.toUiText
import io.github.rojhatince47.weatherchan.feature.favorites.domain.repository.FavoriteRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            unitRepository.unit.collect {
                filter(_uiState.value.query)
            }
        }
        viewModelScope.launch {
            favoriteRepository.favoriteIds.collect {
                filter(_uiState.value.query)
            }
        }
    }

    fun findCity(cityId: Long): City? = loadedCities[cityId]

    fun onQueryChange(query: String) {
        viewModelScope.launch {
            filter(query = query)
        }
    }

    fun onFavoritesOnlyChange(showFavoritesOnly: Boolean) {
        viewModelScope.launch {
            filter(showFavoritesOnly = showFavoritesOnly)
        }
    }

    fun onRetry() {
        load()
    }

    fun onRefresh() {
        load()
    }

    fun onFavoriteClick(cityId: Long) {
        if (cityId !in loadedCities) return
        favoriteRepository.toggleFavorite(cityId)
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(content = UiState.Loading) }
            filter(_uiState.value.query)
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
    }
}

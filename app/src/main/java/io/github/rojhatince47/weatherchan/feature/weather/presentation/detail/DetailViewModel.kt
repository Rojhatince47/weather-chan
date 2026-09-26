package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.state.LoadState
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.state.toLoadState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.text.toUiText
import io.github.rojhatince47.weatherchan.feature.favorites.domain.repository.FavoriteRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetForecastUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getForecast: GetForecastUseCase,
    private val uiMapper: WeatherUiMapper,
    private val unitRepository: TemperatureUnitRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    // Geçersiz/eksik navigation parametresi uygulamayı çökertmemeli;
    // bunun yerine anlaşılır bir hata durumu gösterilir.
    private val city: City? = readCity(savedStateHandle)

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            unitRepository.unit.collect {
                loadData()
            }
        }
        viewModelScope.launch {
            favoriteRepository.favoriteIds.collect {
                loadData()
            }
        }
    }

    fun onRetry() {
        loadData()
    }

    fun onRefresh() {
        loadData()
    }

    fun onFavoriteClick() {
        city?.let { favoriteRepository.toggleFavorite(it.id) }
    }

    fun loadData() {
        _loadState.update { LoadState.Loading }
        viewModelScope.launch {
            _uiState.update { it.copy(content = UiState.Loading) }
            val city = city
            if (city == null) {
                _uiState.update {
                    it.copy(
                        content = UiState.Error(
                            UiText.Dynamic("Şehir bilgisi bulunamadı")
                        )
                    )
                }
                _loadState.update { _uiState.value.content.toLoadState() }
                return@launch
            }
            val result = getForecast(city.id)
            val unit = unitRepository.unit.value
            _uiState.update {
                it.copy(
                    content = when (result) {
                        is AppResult.Success -> UiState.Success(
                            uiMapper.toForecastDetailUiModel(
                                forecast = result.data,
                                isFavorite = city.id in favoriteRepository.favoriteIds.value,
                                unit = unit
                            )
                        )
                        is AppResult.Failure -> UiState.Error(result.error.toUiText())
                    }
                )
            }
            _loadState.update { _uiState.value.content.toLoadState() }
        }
    }

    private fun readCity(savedStateHandle: SavedStateHandle): City? {
        val cityId: Long = savedStateHandle["cityId"] ?: return null
        val name: String = savedStateHandle["name"] ?: return null
        val latitude: Double = savedStateHandle["latitude"] ?: return null
        val longitude: Double = savedStateHandle["longitude"] ?: return null
        return City(
            id = cityId,
            name = name,
            region = savedStateHandle["region"],
            country = savedStateHandle["country"],
            coordinates = Coordinates(
                latitude = latitude,
                longitude = longitude
            )
        )
    }
}

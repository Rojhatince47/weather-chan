package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
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
    private val uiMapper: WeatherUiMapper
) : ViewModel() {

    private val city = City(
        id = checkNotNull(savedStateHandle["cityId"]),
        name = checkNotNull(savedStateHandle["name"]),
        region = savedStateHandle["region"],
        country = savedStateHandle["country"],
        coordinates = Coordinates(
            latitude = checkNotNull(savedStateHandle["latitude"]),
            longitude = checkNotNull(savedStateHandle["longitude"])
        )
    )

    private val isFavorite = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() {
        load()
    }

    fun onRefresh() {
        load()
    }

    fun onFavoriteClick() {
        isFavorite.update { !it }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(content = UiState.Loading) }
            val forecast = getForecast(city.id)
            _uiState.update {
                it.copy(
                    content = UiState.Success(
                        uiMapper.toForecastDetailUiModel(
                            forecast = forecast,
                            isFavorite = isFavorite.value
                        )
                    )
                )
            }
        }
    }
}

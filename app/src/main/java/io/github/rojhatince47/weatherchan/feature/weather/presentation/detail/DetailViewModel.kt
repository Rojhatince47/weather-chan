package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
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
    private val unitRepository: TemperatureUnitRepository
) : ViewModel() {

    // Geçersiz/eksik navigation parametresi uygulamayı çökertmemeli;
    // bunun yerine anlaşılır bir hata durumu gösterilir.
    private val city: City? = readCity(savedStateHandle)

    private val isFavorite = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            unitRepository.unit.collect {
                load()
            }
        }
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
            val city = city
            if (city == null) {
                _uiState.update {
                    it.copy(
                        content = UiState.Error(
                            UiText.Dynamic("Şehir bilgisi bulunamadı")
                        )
                    )
                }
                return@launch
            }
            val result = runCatching { getForecast(city.id) }
            val unit = unitRepository.unit.value
            _uiState.update {
                it.copy(
                    content = result.fold(
                        onSuccess = { forecast ->
                            UiState.Success(
                                uiMapper.toForecastDetailUiModel(
                                    forecast = forecast,
                                    isFavorite = isFavorite.value,
                                    unit = unit
                                )
                            )
                        },
                        onFailure = {
                            UiState.Error(
                                UiText.Dynamic("Tahmin yüklenemedi")
                            )
                        }
                    )
                )
            }
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

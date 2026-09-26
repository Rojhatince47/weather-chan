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
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.ForecastDetailUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)

    /** Tek seferlik UI olayları (snackbar). State'e konmaz. */
    val events: Flow<DetailEvent> = _events.receiveAsFlow()

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    private var loadJob: Job? = null

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
        // İçerik varsa mevcut tahminin üzerinde yenile; yoksa tam yükleme yap.
        if (_uiState.value.content !is UiState.Success) {
            loadData()
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val content = loadForecast()
                when (content) {
                    is UiState.Success -> _uiState.update {
                        it.copy(content = content, isRefreshing = false)
                    }
                    is UiState.Error -> {
                        _uiState.update { it.copy(isRefreshing = false) }
                        _events.send(DetailEvent.ShowSnackbar(content.message))
                    }
                    else -> _uiState.update { it.copy(isRefreshing = false) }
                }
            } catch (e: CancellationException) {
                // İptal bir hata değildir; hata durumu olarak sunulmaz.
                throw e
            }
        }
    }

    fun onFavoriteClick() {
        city?.let { favoriteRepository.toggleFavorite(it.id) }
    }

    fun loadData() {
        // Önceki yükleme varsa iptal edilir; yeniden deneme her zaman
        // yeni bir çağrı başlatır ve eşzamanlı yinelenen istek oluşmaz.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(content = UiState.Loading) }
            _loadState.update { LoadState.Loading }
            try {
                val content = loadForecast()
                _uiState.update { it.copy(content = content) }
                _loadState.update { _uiState.value.content.toLoadState() }
            } catch (e: CancellationException) {
                // İptal bir hata değildir; hata durumu olarak sunulmaz.
                throw e
            }
        }
    }

    private suspend fun loadForecast(): UiState<ForecastDetailUiModel> {
        val city = city
            ?: return UiState.Error(UiText.Dynamic("Şehir bilgisi bulunamadı"))
        val result = getForecast(city.id)
        val unit = unitRepository.unit.value
        return when (result) {
            is AppResult.Success -> UiState.Success(
                uiMapper.toForecastDetailUiModel(
                    forecast = result.data,
                    isFavorite = city.id in favoriteRepository.favoriteIds.value,
                    unit = unit
                )
            )
            is AppResult.Failure -> UiState.Error(result.error.toUiText())
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

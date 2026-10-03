package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.toUiText
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetForecastUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.detail.DetailUiState
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
class EvSayfasiViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    private val getForecast: GetForecastUseCase,
    private val unitRepository: TemperatureUnitRepository,
    private val uiMapper: WeatherUiMapper
) : ViewModel() {

    /** Şehir başına uçuştaki tahmin işleri; eşzamanlı yinelenen istek engellenir. */
    private val forecastJobs = mutableMapOf<Long, Job>()

    /** Birim değişince yeniden eşleme yapmak için son başarılı tahminler. */
    private val forecastCache = mutableMapOf<Long, Forecast>()

    private val _uiState = MutableStateFlow(EvSayfasiUiState())
    val uiState: StateFlow<EvSayfasiUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cityRepository.cities.collect { cities ->
                val pages = buildPages(cities)
                _uiState.update { state ->
                    state.copy(
                        pages = pages,
                        currentPage = clampPage(state.currentPage, pages.size)
                    )
                }
                loadVisibleForecasts()
            }
        }
        viewModelScope.launch {
            unitRepository.unit.collect { remapCachedForecasts() }
        }
    }

    /** Listenin seçimi pager'a taşıdığı id; sayfa index'ine çevrilir. */
    fun selectCity(cityId: Long) {
        val index = _uiState.value.pages.indexOfFirst { it.id == cityId }
        if (index >= 0) {
            _uiState.update { it.copy(currentPage = index) }
            loadVisibleForecasts()
        }
    }

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = clampPage(page, it.pages.size)) }
        loadVisibleForecasts()
    }

    fun onRetry(cityId: Long) {
        loadForecast(cityId, force = true)
    }

    fun onRefresh(cityId: Long) {
        loadForecast(cityId, force = true)
    }

    /**
     * Yalnız görünür sayfa ve birer komşusu çekilir; uzak sayfalar
     * kaydırılana kadar ağa çıkmaz. Spec §9: N şehir = N tur kabul EDİLMEDİ.
     */
    private fun loadVisibleForecasts() {
        val state = _uiState.value
        if (state.pages.isEmpty()) return
        val window = listOf(state.currentPage - 1, state.currentPage, state.currentPage + 1)
            .filter { it in state.pages.indices }
            .distinct()
        window.forEach { index ->
            loadForecast(state.pages[index].id, force = false)
        }
    }

    private fun loadForecast(cityId: Long, force: Boolean) {
        val city = _uiState.value.pages.find { it.id == cityId } ?: return
        if (cityId in forecastJobs) return
        if (!force && forecastCache.containsKey(cityId)) return
        forecastJobs[cityId] = viewModelScope.launch {
            try {
                _uiState.update { state ->
                    val existing = state.forecasts[cityId]
                    state.copy(
                        forecasts = state.forecasts + (
                            cityId to (existing ?: DetailUiState(content = UiState.Loading))
                            )
                    )
                }
                when (val result = getForecast(city)) {
                    is AppResult.Success -> {
                        forecastCache[cityId] = result.data
                        _uiState.update { state ->
                            val ui = uiMapper.toForecastDetailUiModel(
                                forecast = result.data,
                                unit = unitRepository.unit.value
                            )
                            state.copy(
                                forecasts = state.forecasts + (
                                    cityId to DetailUiState(content = UiState.Success(ui))
                                    )
                            )
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update { state ->
                            state.copy(
                                forecasts = state.forecasts + (
                                    cityId to DetailUiState(
                                        content = UiState.Error(result.error.toUiText())
                                    )
                                    )
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                forecastJobs.remove(cityId)
            }
        }
    }

    /** Birim değişimi ağa çıkmaz; önbellekteki tahmin yeniden eşlenir. */
    private fun remapCachedForecasts() {
        val unit = unitRepository.unit.value
        _uiState.update { state ->
            state.copy(
                forecasts = state.forecasts.mapValues { (cityId, detail) ->
                    val cached = forecastCache[cityId]
                    if (cached != null) {
                        DetailUiState(
                            content = UiState.Success(
                                uiMapper.toForecastDetailUiModel(cached, unit = unit)
                            )
                        )
                    } else {
                        detail
                    }
                }
            )
        }
    }
}

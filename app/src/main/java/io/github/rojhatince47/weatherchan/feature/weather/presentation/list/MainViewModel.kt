package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.state.LoadState
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.state.toLoadState
import io.github.rojhatince47.weatherchan.core.ui.text.toUiText
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel
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
class MainViewModel @Inject constructor(
    private val getCityWeathers: GetCityWeathersUseCase,
    private val uiMapper: WeatherUiMapper,
    private val unitRepository: TemperatureUnitRepository,
    private val cityRepository: CityRepository
) : ViewModel() {

    private val loadedCities = mutableMapOf<Long, City>()
    private var loadJob: Job? = null

    /**
     * Son başarılı ağ cevabı. Sorgu bu önbellekten türetilir; her tuş vuruşunda
     * ağa gitmek, kontrollü bir metin alanında yazılan karakteri ağ turu bitene
     * kadar görünmez kılıyordu.
     */
    private var cachedCityWeathers: List<CityWeather>? = null

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = Channel<MainEvent>(Channel.BUFFERED)

    /** Tek seferlik UI olayları (snackbar). State'e konmaz. */
    val events: Flow<MainEvent> = _events.receiveAsFlow()

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            unitRepository.unit.collect { rederiveContent() }
        }
        viewModelScope.launch {
            cityRepository.cities.collect { rederiveContent() }
        }
    }

    fun findCity(cityId: Long): City? = loadedCities[cityId]

    fun onQueryChange(query: String) {
        // Sorgu bir kullanıcı girdisidir, bir veri getirme işlemi değil: seçim ve
        // türetilmiş liste TEK state yazımında güncellenir. Ayrı yazılırsa
        // "seçim yeni, liste eski" diye bir ara kare oluşur ve metin alanı
        // karakteri ağ turu bekliyormuş gibi görünür.
        applySelection(query = query)
    }

    fun onRetry() {
        loadData()
    }

    fun onRefresh() {
        // İçerik varsa mevcut listenin üzerinde yenile; yoksa tam yükleme yap.
        if (_uiState.value.content !is UiState.Success) {
            loadData()
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                when (val result = getCityWeathers(cityRepository.cities.value)) {
                    is AppResult.Success -> {
                        applyFetchedData(result.data)
                        _uiState.update { it.copy(isRefreshing = false) }
                    }
                    // Yenileme başarısızsa eski içerik korunur; kullanıcı bir anda
                    // uyarılır, ekran boşaltılmaz.
                    is AppResult.Failure -> {
                        _uiState.update { it.copy(isRefreshing = false) }
                        _events.send(MainEvent.ShowSnackbar(result.error.toUiText()))
                    }
                }
            } catch (e: CancellationException) {
                // İptal bir hata değildir; hata durumu olarak sunulmaz.
                throw e
            }
        }
    }

    fun onFavoriteClick(cityId: Long) {
        if (cityId !in loadedCities) return
        cityRepository.toggleFavorite(cityId)
    }

    fun loadData() {
        // Önceki yükleme varsa iptal edilir; yeniden deneme her zaman
        // yeni bir çağrı başlatır ve eşzamanlı yinelenen istek oluşmaz.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(content = UiState.Loading) }
            _loadState.update { LoadState.Loading }
            try {
                when (val result = getCityWeathers(cityRepository.cities.value)) {
                    is AppResult.Success -> applyFetchedData(result.data)
                    is AppResult.Failure -> _uiState.update {
                        it.copy(content = UiState.Error(result.error.toUiText()))
                    }
                }
                _loadState.update { _uiState.value.content.toLoadState() }
            } catch (e: CancellationException) {
                // İptal bir hata değildir; hata durumu olarak sunulmaz.
                throw e
            }
        }
    }

    /** Ağdan gelen veriyi önbelleğe alır ve listeyi mevcut seçimlerle türetir. */
    private fun applyFetchedData(cityWeathers: List<CityWeather>) {
        cachedCityWeathers = cityWeathers
        loadedCities.clear()
        loadedCities.putAll(cityWeathers.associate { it.city.id to it.city })
        rederiveContent()
    }

    /**
     * Sorguyu ve ondan türeyen listeyi tek yazımda günceller. Ağ çağrısı yapmaz;
     * bu yüzden yazmak anında yansır. Veri henüz yoksa yalnızca sorgu yazılır,
     * liste yükleme tamamlanınca türetilir.
     */
    private fun applySelection(query: String = _uiState.value.query) {
        val source = cachedCityWeathers
        if (source == null) {
            _uiState.update { it.copy(query = query) }
            return
        }
        val content = deriveContent(source, query)
        _uiState.update { it.copy(query = query, content = content) }
        _loadState.update { _uiState.value.content.toLoadState() }
    }

    /**
     * Listeyi önbellekten türetir. Ağ çağrısı yapmaz; birim ve şehir listesi
     * değişiklikleri bu yolu tetikler. Veri henüz yoksa mevcut içeriğe dokunmaz.
     */
    private fun rederiveContent() {
        val source = cachedCityWeathers ?: return
        val content = deriveContent(source, _uiState.value.query)
        _uiState.update { it.copy(content = content) }
        _loadState.update { _uiState.value.content.toLoadState() }
    }

    /**
     * Favori durumu önbellekteki değil canlı [CityRepository] durumundan okunur:
     * ağ verisi çekildikten sonra yıldız değişirse önbellekteki City nesneleri
     * bayat kalırdı. Sıralama favorileri en üste alır.
     */
    private fun deriveContent(source: List<CityWeather>, query: String): UiState<List<CityWeatherUiModel>> {
        val citiesById = cityRepository.cities.value.associateBy { it.id }
        val unit = unitRepository.unit.value
        val filtered = source
            .filter { cityWeather ->
                query.isBlank() ||
                    cityWeather.city.name.contains(query, ignoreCase = true)
            }
            .sortedByDescending { cityWeather ->
                citiesById[cityWeather.city.id]?.isFavorite == true
            }

        return if (filtered.isEmpty()) {
            UiState.Empty
        } else {
            UiState.Success(
                filtered.map { cityWeather ->
                    uiMapper.toCityWeatherUiModel(
                        cityWeather,
                        isFavorite = citiesById[cityWeather.city.id]?.isFavorite == true,
                        unit = unit
                    )
                }
            )
        }
    }
}

package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.state.LoadState
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.state.toLoadState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.text.toUiText
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.SearchCitiesUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    private val searchCities: SearchCitiesUseCase,
    private val uiMapper: WeatherUiMapper,
    private val unitRepository: TemperatureUnitRepository,
    private val cityRepository: CityRepository
) : ViewModel() {

    private val loadedCities = mutableMapOf<Long, City>()
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    /**
     * Son başarılı ağ cevabı. Sorgu bu önbellekten türetilir; her tuş vuruşunda
     * ağa gitmek, kontrollü bir metin alanında yazılan karakteri ağ turu bitene
     * kadar görünmez kılıyordu.
     */
    private var cachedCityWeathers: List<CityWeather>? = null

    /**
     * Depodan **yayılan** son liste.
     *
     * Başlangıçta boştur ve yalnız `cityRepository.cities` yayımıyla ya da
     * mutasyonların döndürdüğü yazılan listeyle dolar. Depo artık `StateFlow`
     * olmadığı için "uydurma tohum değeri" diye bir yarış kaynağı yoktur;
     * buradaki değer her zaman depodan gelmiştir.
     */
    private var storedCities: List<City> = emptyList()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = Channel<MainEvent>(Channel.BUFFERED)

    /** Tek seferlik UI olayları (snackbar). State'e konmaz. */
    val events: Flow<MainEvent> = _events.receiveAsFlow()

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    init {
        viewModelScope.launch {
            unitRepository.unit.collect { rederiveContent() }
        }
        // Depo **yayımladığı anda** veri çekilir. `loadData()` burada ayrıca
        // çağrılmaz: depodan bir değer okumak yarış yaratırdı, çünkü yayım
        // gelene kadar listenin ne olduğu bilinmez. Akışın ilk yayımı zaten
        // gerçek veridir, dolayısıyla ilk yükleme de doğru listeden başlar.
        viewModelScope.launch {
            cityRepository.cities.collect { stored ->
                storedCities = stored
                refreshSearchInListFlags()
                fetch(stored, keepContent = _uiState.value.content is UiState.Success)
            }
        }
    }

    companion object {
        const val MIN_SEARCH_QUERY_LENGTH = 3
        const val SEARCH_DEBOUNCE_MS = 300L
    }

    fun findCity(cityId: Long): City? = loadedCities[cityId]

    fun onQueryChange(query: String) {
        // Sorgu bir kullanıcı girdisidir, bir veri getirme işlemi değil: seçim ve
        // türetilmiş liste TEK state yazımında güncellenir. Ayrı yazılırsa
        // "seçim yeni, liste eski" diye bir ara kare oluşur ve metin alanı
        // karakteri ağ turu bekliyormuş gibi görünür.
        applySelection(query = query)
        searchLive(query)
    }

    fun onRetry() {
        loadData()
    }

    /** Sürükle-bırak yenileme; içerik varsa korunur, yoksa tam yükleme yapılır. */
    fun onRefresh() {
        if (_uiState.value.content !is UiState.Success) {
            loadData()
            return
        }
        fetch(storedCities, keepContent = true)
    }

    /**
     * Hava durumunu çeker ve listeyi türetir.
     *
     * [keepContent] true ise mevcut içerik **korunur** ve yalnız üstteki
     * yenileme göstergesi döner; başarısızlık ekranı boşaltmaz, snackbar gösterir.
     * false ise ilk yüklemedir: ekran yükleme durumuna alınır ve hata görünür.
     */
    private fun fetch(stored: List<City>, keepContent: Boolean) {
        // Önceki yükleme varsa iptal edilir; her çağrı yeni bir tur başlatır ve
        // eşzamanlı yinelenen istek oluşmaz.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (keepContent) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(content = UiState.Loading) }
                _loadState.update { LoadState.Loading }
            }
            try {
                when (val result = getCityWeathers(stored)) {
                    is AppResult.Success -> {
                        applyFetchedData(result.data)
                        _uiState.update { it.copy(isRefreshing = false) }
                    }
                    is AppResult.Failure -> if (keepContent) {
                        // Yenileme başarısızsa eski içerik korunur; kullanıcı bir
                        // anda uyarılır, ekran boşaltılmaz.
                        _uiState.update { it.copy(isRefreshing = false) }
                        _events.send(MainEvent.ShowSnackbar(result.error.toUiText()))
                    } else {
                        _uiState.update { it.copy(content = UiState.Error(result.error.toUiText())) }
                    }
                }
                _loadState.update { _uiState.value.content.toLoadState() }
            } catch (e: CancellationException) {
                // İptal bir hata değildir; hata durumu olarak sunulmaz.
                throw e
            }
        }
    }

    /**
     * Canlı sonuçtan şehri listeye ekler.
     *
     * Yenileme **bilerek burada yapılmaz**: yazma, depoyu yayımlar ve
     * `init`'deki toplayıcı yeni listeden veri çekmeyi tek yerde başlatır.
     * Ekleme hem elle tetikleseydi her değişiklikte iki tur ağ isteği atılır
     * ve hız sınırına çarpma riski ikiye katlanırdı.
     */
    fun onAddCity(city: City) {
        viewModelScope.launch { cityRepository.add(city) }
    }

    /**
     * Şehri kalıcı listeden siler; geri alma için son durumdaki City snackbar
     * aksiyonunda kapatılır.
     */
    fun onDeleteCity(cityId: Long) {
        val deleted = storedCities.firstOrNull { it.id == cityId } ?: return
        viewModelScope.launch { cityRepository.remove(cityId) }
        _events.trySend(
            MainEvent.ShowSnackbar(
                message = UiText.Resource(R.string.city_deleted, deleted.name),
                actionLabel = UiText.Resource(R.string.undo_delete),
                action = { onUndoDelete(deleted) }
            )
        )
    }

    /** Snackbar "Geri al" aksiyonunun hedefi: silinen şehri geri ekler. */
    fun onUndoDelete(city: City) {
        viewModelScope.launch { cityRepository.add(city) }
    }

    fun loadData() {
        fetch(storedCities, keepContent = false)
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
     * Sorgu değişiminde debounce'lu canlı arama tetiklenir. 3 karakterin
     * altındaki sorgular ağa gitmez, bekleyen istek iptal edilir.
     */
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
                is AppResult.Success -> {
                    val cityIds = storedCities.mapTo(mutableSetOf()) { it.id }
                    _uiState.update {
                        it.copy(
                            searchState = SearchUiState.Success(
                                result.data.map { city ->
                                    SearchResultUiModel(city, city.id in cityIds)
                                }
                            )
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(searchState = SearchUiState.Failure(result.error))
                }
            }
        }
    }

    /**
     * Liste değişince (ekleme/çıkarma) canlı sonuçlardaki
     * "listede mi" bayrakları tazelenir; istek tekrarlanmaz.
     */
    private fun refreshSearchInListFlags() {
        val state = _uiState.value.searchState
        if (state is SearchUiState.Success) {
            val cityIds = storedCities.mapTo(mutableSetOf()) { it.id }
            _uiState.update {
                it.copy(
                    searchState = state.copy(
                        results = state.results.map { result ->
                            result.copy(isInList = result.city.id in cityIds)
                        }
                    )
                )
            }
        }
    }

    /**
     * Canlı depodan okuma ve kayıt sırasının korunması burada toplanır:
     * ağ verisi önbellekten gelir, depo doğruluk kaynağıdır.
     */
    private fun deriveContent(source: List<CityWeather>, query: String): UiState<List<CityWeatherUiModel>> {
        val citiesById = storedCities.associateBy { it.id }
        val unit = unitRepository.unit.value
        val filtered = source
            // Depo doğruluk kaynağıdır: hava durumu önbelleği silinen şehri
            // bir süre daha taşır. Kesişim olmazsa silinen şehir ekranda kalır.
            .filter { cityWeather -> cityWeather.city.id in citiesById }
            .filter { cityWeather ->
                query.isBlank() ||
                    cityWeather.city.name.contains(query, ignoreCase = true)
            }

        return if (filtered.isEmpty()) {
            UiState.Empty
        } else {
            UiState.Success(
                filtered.map { cityWeather ->
                    uiMapper.toCityWeatherUiModel(
                        cityWeather,
                        unit = unit
                    )
                }
            )
        }
    }
}

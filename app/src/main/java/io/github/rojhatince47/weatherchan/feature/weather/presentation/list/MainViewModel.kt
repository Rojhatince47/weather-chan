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
import io.github.rojhatince47.weatherchan.feature.city.domain.permission.CityPermissions
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetLocationCityUseCase
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
    private val cityRepository: CityRepository,
    private val getLocationCity: GetLocationCityUseCase,
    private val cityPermissions: CityPermissions
) : ViewModel() {

    private val loadedCities = mutableMapOf<Long, City>()
    private var loadJob: Job? = null

    /** Şehir başına yeniden deneme işleri; tüm liste yüklemesinden bağımsızdır. */
    private val retryJobs = mutableMapOf<Long, Job>()
    private var locationJob: Job? = null
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
        viewModelScope.launch { restoreLocationPermissionState() }
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

    /**
     * Şehir araması **depoya** düşer; yalnız başarıyla yüklenenlere değil.
     *
     * Yüklenemeyen şehrin kartı da çizilir ve tıklanabilir olmalı (detay
     * ekranı kendi hata durumunu gösterir). `loadedCities` yalnız başarılı
     * fetch'ten dolduğu için tek başına yeterli değildir: yüksüz kartın
     * tıklaması sessizce yutulurdu.
     */
    fun findCity(cityId: Long): City? = loadedCities[cityId] ?: storedCities.firstOrNull { it.id == cityId }

    // --- konum ---------------------------------------------------------------

    /**
     * Bayrak **yalnız "diyalog bir kez gösterildi"** bilgisini taşır, izin
     * cevabını değil. Bu yüzden cevap bayrağa bakılarak tahmin edilmez:
     *
     * - Daha önce sorulmamışsa → [LocationPermissionState.NotAsked]; UI bir kez
     *   sistem diyaloğunu başlatır.
     * - Daha önce sorulmuşsa → diyalog **yeniden açılmaz**, konum doğrudan
     *   okunmaya çalışılır. Gerçek izin [GetLocationCityUseCase] içinde sistemde
     *   sınanır; izin yoksa akış `null` döner ve [LocationState.Unavailable]
     *   gösterilir.
     *
     * Böylece "diyalogu bir kez gördüm" ile "izni veriyorum" karıştırılmaz.
     */
    private suspend fun restoreLocationPermissionState() {
        if (cityPermissions.wasAsked()) {
            _uiState.update { it.copy(permissionState = LocationPermissionState.Asked) }
            refreshLocation()
        } else {
            // Okuma bitti ve diyaloğun hiç gösterilmediği kesinleşti: ancak
            // şimdi `NotAsked`'a geçilir, UI de diyaloğu bir kez başlatır.
            _uiState.update { it.copy(permissionState = LocationPermissionState.NotAsked) }
        }
    }

    /**
     * Sistem diyaloğunun cevabı. Diyaloğun gösterildiği an [markAsked] ile
     * kalıcılaştırılır; cevabın ne olduğu bayrağa yazılmaz.
     */
    fun onPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            cityPermissions.markAsked()
            _uiState.update {
                it.copy(
                    permissionState = if (granted) {
                        LocationPermissionState.Granted
                    } else {
                        LocationPermissionState.Denied
                    }
                )
            }
            if (granted) {
                refreshLocation()
            } else {
                _uiState.update { it.copy(locationState = LocationState.Unavailable) }
            }
        }
    }

    /**
     * Konumu okur ve listeye **konum olarak** ekler.
     *
     * Yazma kasıtlı olarak ayrı bir tur başlatmaz: `addAsLocation` depoyu
     * yayımlar ve `init`'deki toplayıcı yeni listeden veri çekmeyi tek yerde
     * başlatır. Burada ayrıca `fetch` çağırmak her eklemede iki ağ turu
     * demek olurdu.
     */
    fun refreshLocation() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            _uiState.update { it.copy(locationState = LocationState.Loading) }
            val city = getLocationCity()
            if (city == null) {
                // Konum okunamadı: izin yok, GPS kapalı ya da sinyal yok.
                // Akış durmaz, kullanıcı boş durumla karşılaşır.
                _uiState.update { it.copy(locationState = LocationState.Unavailable) }
                return@launch
            }
            cityRepository.addAsLocation(city)
            _uiState.update { it.copy(locationState = LocationState.Resolved) }
        }
    }

    /** Konum kartı silindikten sonra "Konumumu ekle" ile geri getirilir. */
    fun onAddLocation() {
        refreshLocation()
    }

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
        if (deleted.isLocation) {
            // Konum kartı silindi. Durum `Resolved` kalırsa "Konumumu ekle"
            // satırı hiç görünmez ve kullanıcı konumu yalnız uygulamayı
            // yeniden başlatarak geri getirebilirdi.
            _uiState.update { it.copy(locationState = LocationState.Unavailable) }
        }
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

    /**
     * Yalnız bir şehrin hava durumunu yeniden dener.
     *
     * `loadJob` bilinçli olarak kullanılmaz: tek bir şehrin denemesi tüm
     * listenin yüklenmesini iptal etmemeli. İki şehir eşzamanlı denenebilir;
     * bu yüzden işler şehir başına tutulur.
     *
     * Başarısızlık sessizce yutulur çünkü kartın kendisi zaten durumu anlatır:
     * "Hava durumu alınamadı" yazısı kalır. Snackbar atmak hatayı iki kez
     * bildirmek olurdu.
     */
    fun onRetryCity(cityId: Long) {
        val city = storedCities.firstOrNull { it.id == cityId } ?: return
        if (cityId in retryJobs) return
        retryJobs[cityId] = viewModelScope.launch {
            _uiState.update { it.copy(retryingCityIds = it.retryingCityIds + cityId) }
            try {
                when (val result = getCityWeathers(listOf(city))) {
                    is AppResult.Success -> if (result.data.isNotEmpty()) {
                        mergeCityWeathers(result.data)
                    }
                    is AppResult.Failure -> Unit // kart Unavailable kalır
                }
            } finally {
                retryJobs.remove(cityId)
                _uiState.update { it.copy(retryingCityIds = it.retryingCityIds - cityId) }
            }
        }
    }

    /**
     * Tek bir şehrin sonucunu önbelleğe birleştirir. Mevcut liste korunur;
     * yalnız aynı id'li kayıt yerini yenisiyle değiştirir.
     * [applyFetchedData] yeniden kullanılır: `loadedCities` (detay ekranı
     * gezinmesi bunu okur) böylece tutarsız kalmaz.
     *
     * Eşzamanlılık notu: retry'dan önce başlamış bir tam liste yüklemesi
     * retry'dan sonra biterse önbelleği bütünüyle değiştirir ("son yazan
     * kazanır"). Bu bilinçli bir seçimdir: taze bir tam liste, tek şehrin
     * eski sonucundan daha güvenilirdir. Kısa bir "düzeldi sonra bozuldu"
     * penceresi oluşabilir; bir sonraki yenileme kendini düzeltir.
     */
    private fun mergeCityWeathers(newWeathers: List<CityWeather>) {
        val merged = cachedCityWeathers.orEmpty()
            .filterNot { existing ->
                newWeathers.any { new -> new.city.id == existing.city.id }
            } + newWeathers
        applyFetchedData(merged)
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
     * Depo doğruluk kaynağıdır: liste depodaki kayıtlardan kurulur, ağ verisi
     * yalnız doldurucudur.
     *
     * Eski kurulum kesişim alıyordu ve yüklenemeyen şehir listeden sessizce
     * kayboluyordu — kullanıcı eklediği şehrin "silindiğini" sanıyordu.
     * Yeni kurulumda her kayıt yer alır; hava durumu yoksa kart
     * [io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CardWeather.Unavailable]
     * olarak çizilir. Silinen şehir ise depoda olmadığı için doğal olarak
     * görünmez.
     */
    private fun deriveContent(source: List<CityWeather>, query: String): UiState<List<CityWeatherUiModel>> {
        val weathersByCityId = source.associateBy { it.city.id }
        val unit = unitRepository.unit.value
        val visible = storedCities
            .filter { city ->
                query.isBlank() || city.name.contains(query, ignoreCase = true)
            }
            // Konum her zaman başta. Kaynağın sırasına güvenilmez: ağ cevabı
            // koordinat sırasına göre karışık gelebilir. Sıralama, listenin
            // gerçek sahibi olan depodaki kayıt sırasından okunur.
            // `sortedBy` kararlıdır: konum dışındakiler depodaki sıralarını korur.
            .sortedBy { if (it.isLocation) 0 else 1 }

        if (visible.isEmpty()) return UiState.Empty

        return UiState.Success(
            visible.map { city ->
                weathersByCityId[city.id]?.let { uiMapper.toCityWeatherUiModel(it, unit = unit) }
                    ?: uiMapper.toUnavailable(city)
            }
        )
    }
}

package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.toUiText
import io.github.rojhatince47.weatherchan.feature.city.domain.permission.CityPermissions
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CachedForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCachedForecastsUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetForecastUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetLocationCityUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.detail.DetailUiState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationPermissionState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    private val getCachedForecasts: GetCachedForecastsUseCase,
    private val unitRepository: TemperatureUnitRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val uiMapper: WeatherUiMapper,
    private val getLocationCity: GetLocationCityUseCase? = null,
    private val cityPermissions: CityPermissions? = null
) : ViewModel() {

    /** Şehir başına uçuştaki tahmin işleri; eşzamanlı yinelenen istek engellenir. */
    private val forecastJobs = mutableMapOf<Long, Job>()

    /** Birim değişince yeniden eşleme yapmak için son başarılı tahminler. */
    private val forecastCache = mutableMapOf<Long, Forecast>()

    /** Bilinen son başarılı çekişin damgaları; tazelik kararı buradan verilir. */
    private val seedFetchedAt = mutableMapOf<Long, Instant>()
    private var locationJob: Job? = null

    /**
     * Sayfası henüz yayımlanmamış bir şehrin bekleyen seçimi. Arama
     * sonucundan büyüteç, şehri depoya ekleyip hemen seçer; yazım ile
     * yayım arasında yarış olduğundan seçim sayfa gelene dek bekletilir.
     */
    private var pendingSelection: Long? = null

    private val _uiState = MutableStateFlow(EvSayfasiUiState())
    val uiState: StateFlow<EvSayfasiUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Tohum, toplayıcı başlamadan önce okunur (sıralama şarttır):
            // sayfalar yayımlanınca tohum anında ekrana basılabilir.
            getCachedForecasts().orEmpty().forEach { (key, cached) ->
                forecastCache[key] = cached.forecast
                seedFetchedAt[key] = cached.fetchedAt
            }
            cityRepository.cities.collect { cities ->
                val pages = buildPages(cities)
                _uiState.update { state ->
                    // Konum kartı listeden silinirse "Konumumu ekle" geri
                    // gelmelidir. `Idle`, okunamadı anlamındaki `Unavailable`
                    // değildir: konumu kullanıcı kendisi sildi, hiçbir şey
                    // başarısız olmadı.
                    val locationRemoved = state.pages.any { it.isLocation } && pages.none { it.isLocation }
                    // Bekleyen seçim, sayfası yayımlanan şehre iner.
                    val pending = pendingSelection
                    val pendingIndex = pending?.let { id -> pages.indexOfFirst { it.id == id } } ?: -1
                    if (pendingIndex >= 0) pendingSelection = null
                    state.copy(
                        pages = pages,
                        currentPage = clampPage(
                            if (pendingIndex >= 0) pendingIndex else state.currentPage,
                            pages.size
                        ),
                        locationState = if (locationRemoved) LocationState.Idle else state.locationState
                    )
                }
                loadVisibleForecasts()
            }
        }
        viewModelScope.launch {
            unitRepository.unit.collect { remapCachedForecasts() }
        }
        viewModelScope.launch {
            restoreLocationPermissionState()
        }
    }

    /** Listenin seçimi pager'a taşıdığı id; sayfa index'ine çevrilir. */
    fun selectCity(cityId: Long) {
        val index = _uiState.value.pages.indexOfFirst { it.id == cityId }
        if (index >= 0) {
            pendingSelection = null
            _uiState.update { it.copy(currentPage = index) }
            loadVisibleForecasts()
        } else {
            // Sayfa henüz yok: şehir eklenmekte olabilir. Seçim bekletilir;
            // sayfalar yayımlandığında gerçekleştirilir.
            pendingSelection = cityId
        }
    }

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = clampPage(page, it.pages.size)) }
        loadVisibleForecasts()
    }

    fun onRetry(cityId: Long) {
        loadForecast(cityId, force = true, isRefresh = false)
    }

    fun onRefresh(cityId: Long) {
        loadForecast(cityId, force = true, isRefresh = true)
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
            loadForecast(state.pages[index].id, force = false, isRefresh = false)
        }
    }

    private fun loadForecast(cityId: Long, force: Boolean, isRefresh: Boolean = false) {
        val city = _uiState.value.pages.find { it.id == cityId } ?: return
        if (cityId in forecastJobs) return
        if (!force) {
            // Bayat da olsa tohum içeriği önce ekrana basılır; yalnız taze
            // kayıt ağ turunu tamamen atlar.
            publishSeed(city)
            if (seedFresh(city)) return
        }
        forecastJobs[cityId] = viewModelScope.launch {
            try {
                _uiState.update { state ->
                    val existing = state.forecasts[cityId]
                    val content = existing?.content
                    state.copy(
                        forecasts = state.forecasts + (
                            cityId to DetailUiState(
                                content = if (isRefresh && content is UiState.Success) content else content ?: UiState.Loading,
                                isRefreshing = isRefresh
                            )
                            )
                    )
                }
                when (val result = getForecast(city)) {
                    is AppResult.Success -> {
                        val key = seedKeyFor(city)
                        val fetchedAt = clock.instant()
                        forecastCache[key] = result.data
                        seedFetchedAt[key] = fetchedAt
                        _uiState.update { state ->
                            val ui = uiMapper.toForecastDetailUiModel(
                                forecast = result.data,
                                unit = unitRepository.unit.value,
                                today = todayOf(fetchedAt)
                            )
                            state.copy(
                                forecasts = state.forecasts + (
                                    cityId to DetailUiState(
                                        content = UiState.Success(ui),
                                        isRefreshing = false
                                    )
                                    )
                            )
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update { state ->
                            val existing = state.forecasts[cityId]
                            val keepContent = existing?.content
                            state.copy(
                                forecasts = state.forecasts + (
                                    cityId to DetailUiState(
                                        content = if (existing?.content is UiState.Success) {
                                            existing.content
                                        } else {
                                            UiState.Error(result.error.toUiText())
                                        },
                                        isRefreshing = false
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

    /** Bilinen son başarılı tahminin yaşı 30 dk altındaysa ağa çıkılmaz (kaynak disk tohumu ya da aynı oturumun ağ sonucu olabilir). */
    private fun seedFresh(city: City): Boolean {
        val fetchedAt = seedFetchedAt[seedKeyFor(city)] ?: return false
        return !CachedForecast.isStale(fetchedAt, clock.instant())
    }

    /**
     * Disk tohumunu ekrana yazar: kayıt UI modeline eşlenir, içerik
     * `Success` olarak yayımlanır. Tohum yoksa sessizce döner — çağıran
     * tur atmaya devam eder.
     */
    private fun publishSeed(city: City) {
        val forecast = forecastCache[seedKeyFor(city)] ?: return
        val fetchedAt = seedFetchedAt[seedKeyFor(city)] ?: clock.instant()
        val ui = uiMapper.toForecastDetailUiModel(
            forecast = forecast,
            unit = unitRepository.unit.value,
            today = todayOf(fetchedAt)
        )
        _uiState.update { state ->
            state.copy(
                forecasts = state.forecasts + (city.id to DetailUiState(content = UiState.Success(ui)))
            )
        }
    }

    /** Önbellek anahtarı: konum sabit anahtarda, diğerleri kimlikte. */
    private fun seedKeyFor(city: City): Long = if (city.isLocation) CachedForecast.LOCATION_KEY else city.id

    /**
     * Çekim anının cihaz saat dilimindeki yerel günü. `LocalDate.ofInstant`
     * bilerek KULLANILMAZ — API 31+; minSdk 26'da `NoSuchMethodError`
     * fırlatır (düşük API'li cihazda yakalandı). `atZone` API 26'dan beri var.
     */
    private fun todayOf(fetchedAt: Instant): LocalDate = fetchedAt.atZone(ZoneId.systemDefault()).toLocalDate()

    /** Birim değişimi ağa çıkmaz; önbellekteki tahmin yeniden eşlenir. */
    private fun remapCachedForecasts() {
        val unit = unitRepository.unit.value
        _uiState.update { state ->
            state.copy(
                forecasts = state.forecasts.mapValues { (cityId, detail) ->
                    val pageCity = state.pages.find { it.id == cityId }
                    val cached = pageCity?.let { forecastCache[seedKeyFor(it)] }
                    val content = detail.content
                    if (cached != null && content is UiState.Success) {
                        val fetchedAt = pageCity?.let { seedFetchedAt[seedKeyFor(it)] } ?: clock.instant()
                        DetailUiState(
                            content = UiState.Success(
                                uiMapper.toForecastDetailUiModel(
                                    cached,
                                    unit = unit,
                                    today = todayOf(fetchedAt)
                                )
                            ),
                            isRefreshing = detail.isRefreshing
                        )
                    } else {
                        detail
                    }
                }
            )
        }
    }

    private suspend fun restoreLocationPermissionState() {
        val permissions = cityPermissions ?: return
        if (permissions.wasAsked()) {
            _uiState.update { it.copy(permissionState = LocationPermissionState.Asked) }
            refreshLocation()
        } else {
            _uiState.update { it.copy(permissionState = LocationPermissionState.NotAsked) }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            val permissions = cityPermissions ?: return@launch
            permissions.markAsked()
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

    fun refreshLocation() {
        if (locationJob?.isActive == true) return
        val locationCity = getLocationCity ?: return
        locationJob = viewModelScope.launch {
            _uiState.update { it.copy(locationState = LocationState.Loading) }
            val city = locationCity()
            if (city == null) {
                _uiState.update { it.copy(locationState = LocationState.Unavailable) }
                return@launch
            }
            cityRepository.addAsLocation(city)
            _uiState.update { it.copy(locationState = LocationState.Resolved) }
        }
    }

    fun onAddLocation() {
        refreshLocation()
    }

    fun onRetryLocation() {
        refreshLocation()
    }
}

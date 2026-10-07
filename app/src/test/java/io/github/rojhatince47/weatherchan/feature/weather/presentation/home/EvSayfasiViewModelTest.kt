package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.city.domain.permission.CityPermissions
import io.github.rojhatince47.weatherchan.feature.city.testing.FakeCityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CachedForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCachedForecastsUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetForecastUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetLocationCityUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationPermissionState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeWeatherRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EvSayfasiViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val istanbul = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
    private val ankara = City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85))

    private fun city(name: String, lat: Double, isLocation: Boolean = false) = City(
        name = name,
        region = "",
        country = "",
        coordinates = Coordinates(lat, 28.0),
        isLocation = isLocation
    )

    private fun evViewModel(
        fakeWeather: FakeWeatherRepository = FakeWeatherRepository().apply { autoCompleteGates = true },
        fakeCities: FakeCityRepository = FakeCityRepository(),
        unitRepository: FakeTemperatureUnitRepository = FakeTemperatureUnitRepository(),
        clock: Clock = Clock.systemDefaultZone()
    ) = EvSayfasiViewModel(
        cityRepository = fakeCities,
        getForecast = GetForecastUseCase(fakeWeather),
        getCachedForecasts = GetCachedForecastsUseCase(fakeWeather),
        unitRepository = unitRepository,
        clock = clock,
        uiMapper = WeatherUiMapper()
    )

    private fun evViewModelWithLocation(
        fakeWeather: FakeWeatherRepository = FakeWeatherRepository().apply { autoCompleteGates = true },
        fakeCities: FakeCityRepository = FakeCityRepository(),
        locationProvider: LocationProvider = FakeLocationProvider(locationCoordinates),
        permissions: CityPermissions = FakeCityPermissions(asked = true),
        unitRepository: FakeTemperatureUnitRepository = FakeTemperatureUnitRepository(),
        clock: Clock = Clock.systemDefaultZone()
    ) = EvSayfasiViewModel(
        cityRepository = fakeCities,
        getForecast = GetForecastUseCase(fakeWeather),
        getCachedForecasts = GetCachedForecastsUseCase(fakeWeather),
        unitRepository = unitRepository,
        clock = clock,
        getLocationCity = GetLocationCityUseCase(locationProvider, fakeWeather),
        cityPermissions = permissions,
        uiMapper = WeatherUiMapper()
    )

    private val fixedNow = Instant.parse("2026-10-02T10:00:00Z")

    private fun fixedClockAt(base: Instant = fixedNow) = Clock.fixed(base, ZoneOffset.UTC)

    private fun forecastOf(city: City) = Forecast(
        city = city,
        currentWeather = CurrentWeather(21, WeatherCondition.CLEAR, 8, 50),
        hourlyForecast = emptyList(),
        dailyForecast = emptyList()
    )

    @Test
    fun `acilista sayfalar konum birinci sirayla yayinlanir`() = runTest(dispatcher) {
        val location = city("Konum", 10.0, isLocation = true)
        val first = city("İstanbul", 41.0)
        val second = city("Ankara", 39.0)
        val viewModel = evViewModel(
            fakeCities = FakeCityRepository(initial = listOf(first, location, second))
        )
        runCurrent()
        val pages = viewModel.uiState.value.pages
        assertEquals(3, pages.size)
        assertTrue(pages[0].isLocation)
        assertEquals(first.name, pages[1].name)
        assertEquals(second.name, pages[2].name)
    }

    @Test
    fun `bos depo bos sayfa listesi yayinlar`() = runTest(dispatcher) {
        val viewModel = evViewModel(fakeCities = FakeCityRepository(initial = emptyList()))
        runCurrent()
        assertTrue(viewModel.uiState.value.pages.isEmpty())
    }

    @Test
    fun `gorunur pencere disindaki sayfalar aga cikmaz`() = runTest(dispatcher) {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        evViewModel(fakeWeather = fakeWeather)
        runCurrent()
        // Varsayılan 5 şehir; sayfa 0'da pencere {0, 1} → yalnız 2 istek.
        assertEquals(2, fakeWeather.forecastRequests.size)
    }

    @Test
    fun `sayfa degisince yeni pencere cekilir`() = runTest(dispatcher) {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val viewModel = evViewModel(fakeWeather = fakeWeather)
        runCurrent()
        fakeWeather.forecastRequests.clear()
        viewModel.onPageChanged(3)
        runCurrent()
        // Sayfa 3'te pencere {2, 3, 4}.
        assertEquals(3, fakeWeather.forecastRequests.size)
    }

    @Test
    fun `selectCity sayfayi secer`() = runTest(dispatcher) {
        val fakeCities = FakeCityRepository()
        val viewModel = evViewModel(fakeCities = fakeCities)
        runCurrent()
        val target = fakeCities.current()[3]
        viewModel.selectCity(target.id)
        assertEquals(3, viewModel.uiState.value.currentPage)
    }

    /**
     * Arama sonucundaki büyüteç, listeye eklenmemiş şehri de açabilmelidir:
     * şehir depoya yazılıncaya kadar seçim bekletilir, sayfa yayımlandığında
     * gerçekleştirilir (ekleme ile seçim arasında yarış vardır).
     */
    @Test
    fun `sayfasi olmayan sehrin secimi sehri gelince gerceklesir`() = runTest(dispatcher) {
        val first = city("İstanbul", 41.0)
        val second = city("Ankara", 39.0)
        val fakeCities = FakeCityRepository(initial = listOf(first, second))
        val viewModel = evViewModel(fakeCities = fakeCities)
        runCurrent()
        assertEquals(0, viewModel.uiState.value.currentPage)

        val yeni = city("Van", 38.5)
        viewModel.selectCity(yeni.id)
        runCurrent()
        assertEquals("Sayfa yokken seçim uygulanmamalı", 0, viewModel.uiState.value.currentPage)

        fakeCities.add(yeni)
        runCurrent()
        assertEquals("Şehir gelince seçim gerçekleşmeli", 2, viewModel.uiState.value.currentPage)
        assertEquals(yeni.id, viewModel.uiState.value.pages[2].id)
    }

    @Test
    fun `sayfa sayisi kuculunce mevcut sayfa sinirlanir`() = runTest(dispatcher) {
        val first = city("İstanbul", 41.0)
        val second = city("Ankara", 39.0)
        val fakeCities = FakeCityRepository(initial = listOf(first, second))
        val viewModel = evViewModel(fakeCities = fakeCities)
        runCurrent()
        viewModel.selectCity(second.id)
        assertEquals(1, viewModel.uiState.value.currentPage)
        fakeCities.remove(second.id)
        runCurrent()
        assertEquals(0, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `birim degisince icerik yeniden eslenir`() = runTest(dispatcher) {
        val unitRepository = FakeTemperatureUnitRepository()
        val fakeCities = FakeCityRepository()
        val viewModel = evViewModel(
            fakeCities = fakeCities,
            unitRepository = unitRepository
        )
        runCurrent()
        val cityId = fakeCities.current().first().id
        val before = viewModel.uiState.value.forecasts[cityId]?.content
        assertTrue(before is UiState.Success)
        val celsiusText = (before as UiState.Success).data.currentWeather.temperatureText
        unitRepository.setUnit(TemperatureUnit.FAHRENHEIT)
        runCurrent()
        val after = viewModel.uiState.value.forecasts[cityId]?.content
        assertTrue(after is UiState.Success)
        val fahrenheitText = (after as UiState.Success).data.currentWeather.temperatureText
        assertTrue(celsiusText != fahrenheitText)
    }

    @Test
    fun `tahmin hatasi UiState Error olarak yayinlanir`() = runTest(dispatcher) {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            failForecast = true
        }
        val fakeCities = FakeCityRepository()
        val viewModel = evViewModel(fakeWeather = fakeWeather, fakeCities = fakeCities)
        runCurrent()
        val cityId = fakeCities.current().first().id
        assertTrue(viewModel.uiState.value.forecasts[cityId]?.content is UiState.Error)
    }

    @Test
    fun `uiState ilk degerde sayfalar ve tahminler birlikte tasir`() = runTest(dispatcher) {
        val fakeCities = FakeCityRepository()
        val viewModel = evViewModel(fakeCities = fakeCities)
        runCurrent()
        val state = viewModel.uiState.value
        assertEquals(fakeCities.current().size, state.pages.size)
        assertTrue(state.forecasts.isNotEmpty())
    }

    /**
     * Konum sayfası GPS seğirtmesiyle başka bir ızgara hücresine (kimliğe)
     * düşse de tahmin önbellekte kalır: aynı yer için yeni ağ turu açılmaz.
     */
    @Test
    fun `konumun kimligi degisse de hava durumu onbellekte kalir`() = runTest(dispatcher) {
        val fakeWeather = FakeWeatherRepository()
        val locationA = City("Konum", "İl", "Ülke", Coordinates(40.0, 29.0), isLocation = true)
        val locationB = City("Konum", "İl", "Ülke", Coordinates(40.0, 29.0), isLocation = true)
        val city = City("Diğer", "İl", "Ülke", Coordinates(41.0, 30.0))
        val fakeCities = FakeCityRepository(initial = listOf(locationA, city))
        val viewModel = evViewModel(
            fakeCities = fakeCities,
            fakeWeather = fakeWeather
        )
        runCurrent()
        fakeWeather.forecastRequests.clear()
        fakeCities.restore(locationB, 0)
        runCurrent()
        assertEquals(0, fakeWeather.forecastRequests.size)
    }

    @Test
    fun `ilk acilista izin sorulmadi durumundadir`() = runTest(dispatcher) {
        val viewModel = evViewModelWithLocation(permissions = FakeCityPermissions(asked = false))
        runCurrent()
        assertEquals(LocationPermissionState.NotAsked, viewModel.uiState.value.permissionState)
        assertEquals(LocationState.Idle, viewModel.uiState.value.locationState)
    }

    /**
     * Kalıcı bayrak okunana kadar izin durumu **karar verilemez** olmalıdır.
     *
     * Yoksa ilk karede durum `NotAsked` görünür ve UI diyaloğu ateşler.
     * Gerçek cihazda böyle oldu: kullanıcı izni reddettikten sonra uygulamayı
     * her açtığında sistem diyaloğu tekrar karşısına çıktı. Bayrağın DataStore
     * okuması ilk kareden önce tamamlanırsa sorun görünmez; tamamlanmazsa
     * olur. Yani hata **zamanlamaya bağlı** olduğu için gözle güvenilmez,
     * durumun kendisiyle engellenmelidir.
     */
    @Test
    fun `bayrak okunmadan once izin durumu karar verilemez`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Boolean>()
        val viewModel = evViewModelWithLocation(permissions = GatedCityPermissions(gate))

        assertEquals(
            "Okuma sürerken UI karar verememeli; aksi halde diyalog yanlışlıkla açılır",
            LocationPermissionState.Restoring,
            viewModel.uiState.value.permissionState
        )

        gate.complete(true)
        runCurrent()

        assertEquals(
            "Bayrak okununca diyaloğa karar verilebilmeli",
            LocationPermissionState.Asked,
            viewModel.uiState.value.permissionState
        )
    }

    @Test
    fun `bayrak varsa diyalog yeniden acilmaz konum dogrudan okunur`() = runTest(dispatcher) {
        val provider = FakeLocationProvider(locationCoordinates)
        val viewModel = evViewModelWithLocation(
            locationProvider = provider,
            permissions = FakeCityPermissions(asked = true)
        )
        runCurrent()

        // Cevap bayrağa yazılmaz; konum okunarak gerçek durum belirlenir.
        assertEquals(LocationPermissionState.Asked, viewModel.uiState.value.permissionState)
        assertEquals("Bayrak varsa konum yine de okunmalı", 1, provider.callCount)
    }

    @Test
    fun `izin verildiginde konum listeye eklenir`() = runTest(dispatcher) {
        val fakeCities = FakeCityRepository(initial = emptyList())
        val viewModel = evViewModelWithLocation(
            fakeCities = fakeCities,
            permissions = FakeCityPermissions(asked = false)
        )
        runCurrent()

        viewModel.onPermissionResult(granted = true)
        runCurrent()

        val stored = fakeCities.current()
        assertEquals("Konum listeye eklenmeli", 1, stored.size)
        assertTrue("Konum olarak işaretlenmeli", stored.single().isLocation)
        assertEquals(LocationState.Resolved, viewModel.uiState.value.locationState)
    }

    @Test
    fun `izin reddedilince konum okunmaz bos durum gosterilir`() = runTest(dispatcher) {
        val provider = FakeLocationProvider(locationCoordinates)
        val fakeCities = FakeCityRepository(initial = emptyList())
        val viewModel = evViewModelWithLocation(
            fakeCities = fakeCities,
            locationProvider = provider,
            permissions = FakeCityPermissions(asked = false)
        )
        runCurrent()

        viewModel.onPermissionResult(granted = false)
        runCurrent()

        assertEquals(LocationPermissionState.Denied, viewModel.uiState.value.permissionState)
        assertEquals(0, provider.callCount)
        assertTrue("Reddedilince listeye eklenmemeli", fakeCities.current().isEmpty())
        assertEquals(LocationState.Unavailable, viewModel.uiState.value.locationState)
    }

    @Test
    fun `konum okunamazsa akis durmaz bos durum gosterilir`() = runTest(dispatcher) {
        val fakeCities = FakeCityRepository(initial = emptyList())
        val viewModel = evViewModelWithLocation(
            fakeCities = fakeCities,
            locationProvider = FakeLocationProvider(coordinates = null)
        )
        runCurrent()

        assertEquals(
            "Konum alınamadıysa akış hata vermemeli",
            LocationState.Unavailable,
            viewModel.uiState.value.locationState
        )
        assertTrue(fakeCities.current().isEmpty())
    }

    @Test
    fun `konum listenin basinda gorunur`() = runTest(dispatcher) {
        val fakeCities = FakeCityRepository(initial = listOf(istanbul, ankara))
        // Ad çözülemezse kullanıcı "Konum" görür; sıralama testi için
        // gerçek adın geldiği durum kurulur.
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            searchResult = AppResult.Success(listOf(ankara.copy(isLocation = true)))
        }
        val viewModel = evViewModelWithLocation(
            fakeCities = fakeCities,
            fakeWeather = fakeWeather,
            locationProvider = FakeLocationProvider(ankara.coordinates)
        )
        runCurrent()

        val names = viewModel.uiState.value.pages.map { it.name }
        assertEquals(
            "Bölge adı tercih edilmeli, kalan sıra korunmalı",
            listOf("İç Anadolu", "İstanbul"),
            names
        )
    }

    @Test
    fun `konum silinince geri ekleme gorunur olur`() = runTest(dispatcher) {
        val konum = ankara.copy(isLocation = true)
        val fakeCities = FakeCityRepository(initial = listOf(istanbul, konum))
        val viewModel = evViewModelWithLocation(
            fakeCities = fakeCities,
            locationProvider = FakeLocationProvider(konum.coordinates)
        )
        runCurrent()
        assertEquals(LocationState.Resolved, viewModel.uiState.value.locationState)

        // Silme artık ince listede yapılır; pager durumu depo yayınından
        // türetir.
        fakeCities.remove(konum.id)
        runCurrent()

        assertEquals(
            // Konumu kullanıcı silmiştir, okuma **başarısız olmamıştır**. Bu
            // yüzden "Tekrar dene" değil "Konumumu ekle" görünmeli: "Tekrar
            // dene" yalnız gerçekten okunamayan konumda anlamlıdır.
            "Konum silinince geri eklenebilmeli, 'Tekrar dene' dememeli",
            LocationState.Idle,
            viewModel.uiState.value.locationState
        )
    }

    /**
     * Yavaş okuma (GPS ~10 sn) sürerken hızlı dokunuşlar turu öldürmemelidir:
     * her dokunuşta iptal-et-baştan-başlat yapılırsa, dokunuş sıklığı okuma
     * süresini aşınca tur hiç bitmez (açlık) ve kullanıcı boşuna bekler.
     */
    @Test
    fun `okuma surerken yeni dokunus onceki turu oldurmez`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Coordinates?>()
        val provider = FakeLocationProvider(coordinates = null, gate = gate)
        val fakeCities = FakeCityRepository(initial = emptyList())
        val viewModel = evViewModelWithLocation(
            fakeCities = fakeCities,
            locationProvider = provider,
            permissions = FakeCityPermissions(asked = false)
        )
        runCurrent()

        viewModel.onAddLocation()
        runCurrent()
        // Hızlı ikinci dokunuş: sürmekte olan tur öldürülmemeli.
        viewModel.onAddLocation()
        runCurrent()
        assertEquals(1, provider.callCount)

        gate.complete(istanbul.coordinates)
        runCurrent()
        assertEquals(LocationState.Resolved, viewModel.uiState.value.locationState)
        assertTrue(fakeCities.current().any { it.isLocation })
    }

    @Test
    fun `konum haricindeki sehir silinince konum durumu degismez`() = runTest(dispatcher) {
        val konum = ankara.copy(isLocation = true)
        val fakeCities = FakeCityRepository(initial = listOf(istanbul, konum))
        val viewModel = evViewModelWithLocation(
            fakeCities = fakeCities,
            locationProvider = FakeLocationProvider(konum.coordinates)
        )
        runCurrent()

        fakeCities.remove(istanbul.id)
        runCurrent()

        assertEquals(
            "Konum hâlâ listede, durum değişmemeli",
            LocationState.Resolved,
            viewModel.uiState.value.locationState
        )
    }

    // --- tohumlama testleri ---------------------------------------------------

    @Test
    fun `taze tohumla gorunur sayfa aga cikmaz`() = runTest(dispatcher) {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            cachedForecasts = mapOf(istanbul.id to CachedForecast(fixedNow, forecastOf(istanbul)))
        }
        val viewModel = evViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(istanbul)),
            clock = fixedClockAt(fixedNow.plusSeconds(10 * 60))
        )
        runCurrent()
        assertTrue(fakeWeather.forecastRequests.isEmpty())
        val page = viewModel.uiState.value.forecasts.getValue(istanbul.id)
        assertTrue(page.content is UiState.Success)
    }

    @Test
    fun `bayat tohumla sessiz tur atilir`() = runTest(dispatcher) {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            cachedForecasts = mapOf(istanbul.id to CachedForecast(fixedNow, forecastOf(istanbul)))
        }
        val viewModel = evViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(istanbul)),
            clock = fixedClockAt(fixedNow.plusSeconds(31 * 60))
        )
        runCurrent()
        // Tur atılır; tohum içerik olarak ekranda kalır (ağ başarısız olsa bile).
        assertEquals(1, fakeWeather.forecastRequests.size)
        assertTrue(viewModel.uiState.value.forecasts.getValue(istanbul.id).content is UiState.Success)
    }

    @Test
    fun `cevrimdisi bayat tohumlu icerik korunur`() = runTest(dispatcher) {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            failForecast = true
            cachedForecasts = mapOf(istanbul.id to CachedForecast(fixedNow, forecastOf(istanbul)))
        }
        val viewModel = evViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(istanbul)),
            clock = fixedClockAt(fixedNow.plusSeconds(31 * 60))
        )
        runCurrent()
        // Bayat tohum yine de ekrana basılır; başarısız tazeleme onu silmez.
        assertEquals(1, fakeWeather.forecastRequests.size)
        assertTrue(viewModel.uiState.value.forecasts.getValue(istanbul.id).content is UiState.Success)
    }

    @Test
    fun `konum sayfasi tohumu sentinelden gelir`() = runTest(dispatcher) {
        // Kimlik dönüşümü: depodaki konum kaydı tohum yazıldığından
        // farklı kimlikte; sayfa yine de tohumlanır, ağa çıkılmaz.
        val storedLocation = City("Konum", null, null, Coordinates(41.03, 29.02), isLocation = true)
        val oldLocation = storedLocation.copy(coordinates = Coordinates(41.00, 28.98))
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            cachedForecasts = mapOf(CachedForecast.LOCATION_KEY to CachedForecast(fixedNow, forecastOf(oldLocation)))
        }
        val viewModel = evViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(storedLocation)),
            clock = fixedClockAt(fixedNow.plusSeconds(10 * 60))
        )
        runCurrent()
        assertTrue(fakeWeather.forecastRequests.isEmpty())
        assertTrue(viewModel.uiState.value.forecasts.getValue(storedLocation.id).content is UiState.Success)
    }

    @Test
    fun `eksik sehir icin tur atilir`() = runTest(dispatcher) {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        evViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(istanbul)),
            clock = fixedClockAt(fixedNow.plusSeconds(10 * 60))
        )
        runCurrent()
        assertEquals(listOf(istanbul), fakeWeather.forecastRequests)
    }

    // --- konum testleri için sahteler -----------------------------------------

    private val locationCoordinates = Coordinates(41.01, 28.98)

    /** Sabit konum döndüren sahte; `null` verirsen konum okunamaz. */
    private class FakeLocationProvider(
        private val coordinates: Coordinates?,
        private val gate: CompletableDeferred<Coordinates?>? = null
    ) : LocationProvider {
        var callCount: Int = 0
            private set

        override suspend fun currentCoordinates(): Coordinates? {
            callCount++
            return gate?.await() ?: coordinates
        }
    }

    /** Kalıcı "diyalog gösterildi" bayrağının sahtesi. */
    private class FakeCityPermissions(private var asked: Boolean = false) : CityPermissions {
        override suspend fun wasAsked(): Boolean = asked

        override suspend fun markAsked() {
            asked = true
        }
    }

    /** Bayrak okumasını istenen ana kadar bekleten sahte. */
    private class GatedCityPermissions(private val gate: CompletableDeferred<Boolean>) : CityPermissions {
        override suspend fun wasAsked(): Boolean = gate.await()

        override suspend fun markAsked() {}
    }
}

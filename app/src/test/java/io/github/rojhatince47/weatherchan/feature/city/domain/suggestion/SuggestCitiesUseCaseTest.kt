package io.github.rojhatince47.weatherchan.feature.city.domain.suggestion

import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.PlaceName
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.ReverseGeocoder
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeWeatherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestCitiesUseCaseTest {

    private val istanbul = CitySeed("İstanbul", "TR", "Türkiye", Coordinates(41.0082, 28.9784))
    private val kocaeli = CitySeed("Kocaeli", "TR", "Türkiye", Coordinates(40.7654, 29.9408))
    private val mardin = CitySeed("Mardin", "TR", "Türkiye", Coordinates(37.3122, 40.7351))
    private val london = CitySeed("Londra", "GB", "Birleşik Krallık", Coordinates(51.5074, -0.1278))
    private val newYork = CitySeed("New York", "US", "Amerika Birleşik Devletleri", Coordinates(40.7128, -74.006))
    private val berlin = CitySeed("Berlin", "DE", "Almanya", Coordinates(52.52, 13.405))

    /** Sabit yanıt döndüren sahte geri çözümleyici. */
    private class FakeReverseGeocoder(private val placeName: PlaceName?) : ReverseGeocoder {
        override suspend fun reverse(coordinates: Coordinates): PlaceName? = placeName
    }

    private suspend fun useCase(
        seeds: List<CitySeed>? = listOf(istanbul, kocaeli, mardin, london, newYork),
        coordinates: Coordinates? = null,
        countryCode: String? = "TR",
        nearbySeeds: List<CitySeed>? = seeds,
        refresh: Boolean = false,
        nearbyEnabled: Boolean = true,
        reverse: PlaceName? = null,
        cachedLocation: City? = null
    ): SuggestionGroup? {
        val provider: CitySuggestionProvider =
            CitySuggestionProvider { seeds?.let { SuggestionData(it, listOf("GB", "US")) } }
        val weather = FakeWeatherRepository().apply { cachedLocationCity = cachedLocation }
        return SuggestCitiesUseCase(
            suggestionProvider = provider,
            nearbyProvider = FakeNearbyCitySuggestionProvider(nearbySeeds),
            locationProvider = LocationProvider { coordinates },
            deviceCountryProvider = DeviceCountryProvider { countryCode },
            reverseGeocoder = FakeReverseGeocoder(reverse),
            weatherRepository = weather
        )(refresh = refresh, nearbyEnabled = nearbyEnabled)
    }

    @Test
    fun `konum varsa yakinlik sirasi kullanilir`() = runTest {
        // Yakınında dalı artık Overpass sağlayıcısından gelir; menşe noktası
        // seed'in **tam** koordinatıdır, mesafe 0 olmalıdır.
        val group = useCase(coordinates = istanbul.coordinates)!!
        assertEquals(SuggestionKind.Nearby, group.kind)
        assertEquals("İstanbul", group.suggestions.first().city.name)
        assertEquals(0, group.suggestions.first().distanceKm)
        assertTrue(group.suggestions.map { it.distanceKm!! }.zipWithNext().all { (a, b) -> a <= b })
        assertTrue(group.suggestions.drop(1).first().distanceKm!! < 250)
    }

    @Test
    fun `yakinlik sirasi saglayici sirasindan bagimsizdir`() = runTest {
        // Sağlayıcı havuzu mesafeye TERS verilir: sıralama olmasaydı ilk öneri
        // Mardin olurdu. (`sortedBy` silinirse bu test kırılır.)
        val group = useCase(nearbySeeds = listOf(mardin, kocaeli, istanbul), coordinates = istanbul.coordinates)!!
        assertEquals(SuggestionKind.Nearby, group.kind)
        assertEquals(listOf("İstanbul", "Kocaeli", "Mardin"), group.suggestions.map { it.city.name })
    }

    @Test
    fun `bilinen mesafe sabitlenir`() = runTest {
        // İstanbul–Kocaeli gerçek mesafesi ~85 km'dir. Derece→radyan dönüşümü
        // silinse hesap tamamen sapar ve bu aralık testi kırar.
        val group = useCase(coordinates = istanbul.coordinates)!!
        val kocaeliDistance = group.suggestions.first { it.city.name == "Kocaeli" }.distanceKm!!
        assertTrue("İstanbul–Kocaeli ~85 km olmalı, geldi: $kocaeliDistance", kocaeliDistance in 80..90)
    }

    @Test
    fun `yakin saglayici bosken ulke dalina duser`() = runTest {
        // Kritik ayrım: dosya havuzu Yakınında için kullanılmaz; sağlayıcı
        // boşsa öncelik zinciri doğrudan Ülkende'ye düşer (dosya sırasıyla).
        val group = useCase(nearbySeeds = null, coordinates = istanbul.coordinates)!!
        assertEquals(SuggestionKind.Country, group.kind)
        assertEquals(listOf("İstanbul", "Kocaeli", "Mardin"), group.suggestions.map { it.city.name })
        assertTrue(group.suggestions.all { it.distanceKm == null })
    }

    @Test
    fun `yakin saglayici bos ulke de yoksa dunya yedegine duser`() = runTest {
        val group = useCase(nearbySeeds = null, coordinates = istanbul.coordinates, countryCode = "ZZ")!!
        assertEquals(SuggestionKind.World, group.kind)
    }

    @Test
    fun `gps ulkesi cihaz ulkesini gecer`() = runTest {
        // Sahte GPS'le ülke değişince Ülkende kovası da değişmeli: cihaz
        // yapılandırma locale'i ("TR") değil, geri çözümlemenin ISO kodu ("DE").
        val group = useCase(
            seeds = listOf(istanbul, berlin),
            coordinates = berlin.coordinates,
            nearbySeeds = null,
            countryCode = "TR",
            reverse = PlaceName(name = "Berlin", region = "Berlin", country = "Almanya", countryCode = "DE")
        )!!
        assertEquals(SuggestionKind.Country, group.kind)
        assertEquals(listOf("Berlin"), group.suggestions.map { it.city.name })
    }

    @Test
    fun `geri cozumleme yoksa cihaz ulkesi kalir`() = runTest {
        val group = useCase(
            seeds = listOf(istanbul, berlin),
            coordinates = berlin.coordinates,
            nearbySeeds = null,
            countryCode = "TR",
            reverse = null
        )!!
        assertEquals(listOf("İstanbul"), group.suggestions.map { it.city.name })
    }

    @Test
    fun `dosya asamasinda son bilinen konum ulkeyi belirler`() = runTest {
        // Dosya aşamasında GPS bilinçli okunmaz (12 sn gecikme yasağı); ülke
        // kodu son bilinen konum kaydından geri çözümlemeyle alınır.
        val group = useCase(
            seeds = listOf(istanbul, berlin),
            coordinates = null,
            nearbySeeds = null,
            countryCode = "TR",
            nearbyEnabled = false,
            cachedLocation = City("Konum", null, null, berlin.coordinates, isLocation = true),
            reverse = PlaceName(name = "Berlin", region = "Berlin", countryCode = "DE")
        )!!
        assertEquals(SuggestionKind.Country, group.kind)
        assertEquals(listOf("Berlin"), group.suggestions.map { it.city.name })
    }

    @Test
    fun `dosya yokken bile yakininda gelir`() = runTest {
        // Dosya (asset) okunamasa bile konum + Overpass öneriyi sürdürür.
        val group = useCase(seeds = null, nearbySeeds = listOf(istanbul), coordinates = istanbul.coordinates)!!
        assertEquals(SuggestionKind.Nearby, group.kind)
        assertEquals("İstanbul", group.suggestions.first().city.name)
    }

    @Test
    fun `refresh canli sorguyu tetikler`() = runTest {
        val fake = FakeNearbyCitySuggestionProvider(result = listOf(istanbul))
        val useCase = SuggestCitiesUseCase(
            suggestionProvider = CitySuggestionProvider { null },
            nearbyProvider = fake,
            locationProvider = LocationProvider { istanbul.coordinates },
            deviceCountryProvider = DeviceCountryProvider { "TR" }
        )
        useCase(refresh = false)
        assertEquals(0, fake.refreshCalls)
        useCase(refresh = true)
        assertEquals(1, fake.refreshCalls)
        assertTrue(fake.origins.isNotEmpty())
    }

    @Test
    fun `konum yoksa saglayici hic cagrilmaz`() = runTest {
        val fake = FakeNearbyCitySuggestionProvider(result = listOf(istanbul))
        val useCase = SuggestCitiesUseCase(
            suggestionProvider = CitySuggestionProvider { null },
            nearbyProvider = fake,
            locationProvider = LocationProvider { null },
            deviceCountryProvider = DeviceCountryProvider { "TR" }
        )
        useCase()
        assertTrue(fake.origins.isEmpty())
        assertEquals(0, fake.refreshCalls)
    }

    @Test
    fun `konum yoksa ulke dali dosya sirasini korur`() = runTest {
        val group = useCase()!!
        assertEquals(SuggestionKind.Country, group.kind)
        assertEquals(listOf("İstanbul", "Kocaeli", "Mardin"), group.suggestions.map { it.city.name })
        assertTrue(group.suggestions.all { it.distanceKm == null })
    }

    @Test
    fun `bilinmeyen ulke kodu dunya yedegine duser`() = runTest {
        val group = useCase(countryCode = "ZZ")!!
        assertEquals(SuggestionKind.World, group.kind)
        assertEquals(listOf("Londra", "New York"), group.suggestions.map { it.city.name })
    }

    @Test
    fun `liste bos ulke kodu dunya yedegine duser`() = runTest {
        assertEquals(SuggestionKind.World, useCase(countryCode = "")!!.kind)
    }

    @Test
    fun `yakininda kapaliyken konum okunmaz ve dosya dalina donulur`() = runTest {
        // Aşama A (dosya-önce): konum 12 sn GPS beklemez, Yakınında hiç
        // denenmez — dosya önerisi anında çıkar (Ö2).
        val fake = FakeNearbyCitySuggestionProvider(result = listOf(istanbul))
        val useCase = SuggestCitiesUseCase(
            suggestionProvider = CitySuggestionProvider { SuggestionData(listOf(istanbul, kocaeli), listOf("GB")) },
            nearbyProvider = fake,
            locationProvider = LocationProvider { error("Aşama A konum okumamalı") },
            deviceCountryProvider = DeviceCountryProvider { "TR" }
        )
        val group = useCase(nearbyEnabled = false)!!
        assertEquals(SuggestionKind.Country, group.kind)
        assertTrue(fake.origins.isEmpty())
    }

    @Test
    fun `veri yoksa grup yoktur`() = runTest {
        val useCase = SuggestCitiesUseCase(
            suggestionProvider = CitySuggestionProvider { null },
            nearbyProvider = FakeNearbyCitySuggestionProvider(result = null),
            locationProvider = LocationProvider { null },
            deviceCountryProvider = DeviceCountryProvider { null }
        )
        assertNull(useCase())
    }

    @Test
    fun `limit uygulanir`() = runTest {
        val many = (1..12).map { CitySeed("Şehir $it", "TR", "Türkiye", Coordinates(40.0 + it, 29.0)) }
        val group = useCase(seeds = many, coordinates = Coordinates(41.0, 29.0))!!
        assertEquals(8, group.suggestions.size)
    }

    @Test
    fun `az kayit varsa kirpma yapilmaz`() = runTest {
        val group = useCase(seeds = listOf(istanbul, kocaeli), coordinates = Coordinates(41.0, 29.0))!!
        assertEquals(2, group.suggestions.size)
    }
}

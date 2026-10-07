package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.rojhatince47.weatherchan.feature.city.data.remote.overpass.OverpassApi
import io.github.rojhatince47.weatherchan.feature.city.data.remote.overpass.OverpassElementDto
import io.github.rojhatince47.weatherchan.feature.city.data.remote.overpass.OverpassResponseDto
import io.github.rojhatince47.weatherchan.feature.city.data.remote.overpass.OverpassSuggestionMapper
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * [OverpassNearbyCitySuggestionProvider] ağ/önbellek davranışları:
 * anti-çift-istek kısa devresi, mirror yedeği, iptal sözleşmesi.
 */
class OverpassNearbyCitySuggestionProviderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val origin = Coordinates(41.0082, 28.9784)

    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var cache: NearbyCachePreferences
    private lateinit var clock: MutableClock
    private lateinit var api: FakeOverpassApi
    private lateinit var provider: OverpassNearbyCitySuggestionProvider

    private data class MutableClock(var instant: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = instant
        fun advance(d: Duration) {
            instant = instant.plus(d)
        }
    }

    private class FakeOverpassApi(
        var response: OverpassResponseDto? = defaultResponse,
        var failUrls: Set<String> = emptySet()
    ) : OverpassApi {
        val calls = mutableListOf<String>()

        override suspend fun query(url: String, data: String): OverpassResponseDto {
            calls += url
            if (failUrls.any { url == it || url.startsWith("$it/") }) throw IOException("ağ hatası")
            return response ?: OverpassResponseDto()
        }

        companion object {
            val defaultResponse = OverpassResponseDto(
                elements = listOf(OverpassElementDto(lat = 41.01, lon = 28.98, tags = mapOf("name" to "Kuzguncuk")))
            )
        }
    }

    @Before
    fun setup() {
        file = folder.newFile("nearby_cache.preferences_pb")
        file.delete()
        dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList<androidx.datastore.core.DataMigration<Preferences>>(),
            scope = kotlinx.coroutines.test.TestScope(kotlinx.coroutines.test.UnconfinedTestDispatcher()).backgroundScope
        ) { file } as DataStore<Preferences>
        clock = MutableClock(Instant.parse("2026-10-06T00:00:00Z"))
        cache = NearbyCachePreferences(dataStore, kotlinx.coroutines.Dispatchers.Unconfined, clock)
        api = FakeOverpassApi()
        provider = OverpassNearbyCitySuggestionProvider(
            api = api,
            cache = cache,
            languageProvider = { "tr" },
            clock = clock,
            mapper = OverpassSuggestionMapper()
        )
    }

    @Test
    fun `ilk tazeleme aga gider ve onbellegi yazar`() = runTest {
        val seeds = provider.refresh(origin)
        assertNotNull(seeds)
        assertEquals(1, api.calls.size)
        assertEquals(listOf("Kuzguncuk"), seeds!!.map { it.name })
    }

    @Test
    fun `hemen ardindaki tazeleme istek tekrarlamaz`() = runTest {
        // Açılış zinciri: nearby() soğuk önbellekte canlı tur atar, ViewModel
        // hemen ardından refresh=true çağırır. İkinci tur aynı sorguyu
        // tekrarlamamalı (anti-çift-istek penceresi 60 sn).
        provider.refresh(origin)
        clock.advance(Duration.ofSeconds(10))
        val second = provider.refresh(origin)
        assertEquals("Aynı anda iki Overpass isteği gitti", 1, api.calls.size)
        assertEquals(listOf("Kuzguncuk"), second!!.map { it.name })
    }

    @Test
    fun `anti cift penceresinden sonraki tazeleme canli tur atar`() = runTest {
        provider.refresh(origin)
        clock.advance(Duration.ofSeconds(61))
        provider.refresh(origin)
        assertEquals(2, api.calls.size)
    }

    @Test
    fun `yakin taze onbellege nearby aga gitmez`() = runTest {
        provider.refresh(origin)
        clock.advance(Duration.ofMinutes(5))
        val seeds = provider.nearby(origin)
        assertEquals(1, api.calls.size)
        assertNotNull(seeds)
    }

    @Test
    fun `birinci mirror hata verince ikincisi denenir`() = runTest {
        api.failUrls = setOf(OverpassNearbyCitySuggestionProvider.MIRRORS.first())
        val seeds = provider.refresh(origin)
        assertEquals(2, api.calls.size)
        assertEquals(OverpassNearbyCitySuggestionProvider.MIRRORS.last() + "/api/interpreter", api.calls.last())
        assertNotNull(seeds)
    }

    @Test
    fun `her iki mirror da dusunce bayat onbellegi dondurur`() = runTest {
        provider.refresh(origin)
        clock.advance(Duration.ofHours(7))
        api.failUrls = OverpassNearbyCitySuggestionProvider.MIRRORS.toSet()
        val seeds = provider.refresh(origin)
        assertEquals(3, api.calls.size)
        // Bayat ama dolu: boştan iyidir (spec §5) — Nearby gösterilmeye devam eder.
        assertEquals(listOf("Kuzguncuk"), seeds!!.map { it.name })
    }

    @Test
    fun `her iki mirror da dusunce ve onbellek yoksa null dondurulur`() = runTest {
        api.failUrls = OverpassNearbyCitySuggestionProvider.MIRRORS.toSet()
        assertEquals(null, provider.refresh(origin))
    }

    @Test
    fun `iptal yutulmaz`() = runTest {
        api.failUrls = emptySet()
        val cancellingApi = object : OverpassApi {
            override suspend fun query(url: String, data: String): OverpassResponseDto = throw CancellationException("iptal")
        }
        val cancellingProvider = OverpassNearbyCitySuggestionProvider(
            api = cancellingApi,
            cache = cache,
            languageProvider = { "tr" },
            clock = clock,
            mapper = OverpassSuggestionMapper()
        )
        // Proje kuralı (bkz. runCatchingApp): iptal "veri yok" sayılmaz.
        var caught: Throwable? = null
        try {
            cancellingProvider.refresh(origin)
        } catch (e: CancellationException) {
            caught = e
        }
        assertNotNull("İptal yutulmamalı", caught)
        assertTrue(api.calls.isEmpty())
    }
}

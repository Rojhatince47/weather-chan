package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySeed
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NearbyCachePreferencesTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var cache: NearbyCachePreferences
    private lateinit var clock: MutableClock

    data class MutableClock(var instant: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = instant
        fun advance(d: Duration) {
            instant = instant.plus(d)
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
        cache = NearbyCachePreferences(dataStore, Dispatchers.Unconfined, clock)
    }

    @After
    fun tearDown() {
        // dataStore.close() not exposed; temp file cleaned
    }

    @Test
    fun `ayni hucrede isabet 6 saat icinde taze sayilir`() = runTest {
        val origin = Coordinates(41.0, 29.0)
        val seeds = listOf(CitySeed("Kuzguncuk", "", "", Coordinates(41.03, 29.03)))
        cache.write(origin, seeds)
        clock.advance(Duration.ofHours(5).plusMinutes(59))
        val read = cache.read(origin)
        assertNotNull(read)
        assertEquals("Kuzguncuk", read!!.single().name)
        assertTrue(cache.isFresh(origin, clock.instant()))
    }

    @Test
    fun `6 saat gecince taze degil`() = runTest {
        val origin = Coordinates(41.0, 29.0)
        val seeds = listOf(CitySeed("Kuzguncuk", "", "", Coordinates(41.03, 29.03)))
        cache.write(origin, seeds)
        clock.advance(Duration.ofHours(6))
        assertTrue(!cache.isFresh(origin, clock.instant()))
        // read still returns (stale-while-error)
        assertNotNull(cache.read(origin))
    }

    @Test
    fun `farkli hucre isabet etmez`() = runTest {
        val origin = Coordinates(41.0, 29.0)
        cache.write(origin, listOf(CitySeed("A", "", "", Coordinates(41.0, 29.0))))
        val other = Coordinates(41.15, 29.15) // farklı hücre (0.1°)
        assertNull(cache.read(other))
    }

    @Test
    fun `ayni hucre butcesi`() = runTest {
        val origin = Coordinates(41.04, 29.04) // hücre (41.0,29.0)
        val origin2 = Coordinates(41.09, 29.09) // hücre aynı
        cache.write(origin, listOf(CitySeed("A", "", "", Coordinates(41.0, 29.0))))
        assertNotNull(cache.read(origin2))
    }
}

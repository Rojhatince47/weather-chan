package io.github.rojhatince47.weatherchan.feature.weather.domain.model

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedWeathersTest {

    private val fetchedAt = Instant.parse("2026-10-02T10:00:00Z")

    @Test
    fun `yirmi dokuzuncu dakikada taze sayilir`() {
        assertFalse(CachedWeathers(fetchedAt, emptyList()).isStale(fetchedAt.plusSeconds(29 * 60 + 59)))
    }

    @Test
    fun `otuzuncu dakikada bayat sayilir`() {
        assertTrue(CachedWeathers(fetchedAt, emptyList()).isStale(fetchedAt.plusSeconds(30 * 60)))
    }

    @Test
    fun `gelecek damga bayat sayilir`() {
        assertTrue(CachedWeathers(fetchedAt, emptyList()).isStale(fetchedAt.minusSeconds(1)))
    }
}

package io.github.rojhatince47.weatherchan.feature.weather.domain.model

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HourlyWindowTest {

    private fun hour(at: LocalDateTime, temp: Int = 15) = HourlyForecast(at, at.hour, temp, WeatherCondition.CLEAR, 0)

    private val start = LocalDateTime.of(2026, 10, 8, 14, 0)

    @Test
    fun `saat hizal 25 og iki ucu dahil kapsar`() {
        val dense = (0L..48L).map { hour(start.plusHours(it), it.toInt()) }
        val window = hourlyWindow(dense, start)
        assertEquals(25, window.size)
        assertEquals(start, window.first().localDateTime)
        assertEquals(start.plusHours(24), window.last().localDateTime)
    }

    @Test
    fun `gece yarisini asan pencere 25 og tutar`() {
        val night = LocalDateTime.of(2026, 10, 8, 23, 0)
        val dense = (0L..48L).map { hour(night.plusHours(it)) }
        val window = hourlyWindow(dense, night)
        assertEquals(25, window.size)
        assertEquals(night, window.first().localDateTime)
    }

    @Test
    fun `seyrek uc saatlik girdi en cok dokuz og uretir`() {
        val sparse = (0L..48L step 3).map { hour(start.plusHours(it)) }
        val window = hourlyWindow(sparse, start)
        assertEquals(9, window.size)
    }

    @Test
    fun `start oncesindeki kayitlar duser`() {
        val items = listOf(hour(start.minusHours(1)), hour(start), hour(start.plusHours(1)))
        val window = hourlyWindow(items, start)
        assertEquals(2, window.size)
        assertEquals(start, window.first().localDateTime)
    }

    @Test
    fun `artiyirmidort tam sinirindaki og iceridedir`() {
        val items = listOf(hour(start), hour(start.plusHours(24)))
        assertEquals(2, hourlyWindow(items, start).size)
    }

    @Test
    fun `bos liste bos pencere uretir`() {
        assertTrue(hourlyWindow(emptyList(), start).isEmpty())
    }

    @Test
    fun `start saat kesimlenir - on dord bucuk on dort ile esdegerdir`() {
        val dense = (0L..48L).map { hour(start.plusHours(it)) }
        assertEquals(hourlyWindow(dense, start), hourlyWindow(dense, start.withMinute(37)))
    }
}

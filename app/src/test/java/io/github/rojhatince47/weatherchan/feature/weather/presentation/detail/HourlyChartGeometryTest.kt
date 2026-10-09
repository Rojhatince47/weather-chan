package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HourlyChartGeometryTest {

    @Test
    fun `min max olcekleme uc degerleri pay icinde tutar`() {
        val points = chartPoints(listOf(-5, 0, 5), cellWidthPx = 64f, chartHeightPx = 96f, padPx = 16f)

        assertEquals(3, points.size)
        assertEquals(32f, points[0].first, 0.01f)
        assertEquals(80f, points[0].second, 0.01f) // min → alt pay
        assertEquals(48f, points[1].second, 0.01f) // orta
        assertEquals(16f, points[2].second, 0.01f) // max → üst pay
    }

    @Test
    fun `esit degerler ortada yatay cizgi olusturur`() {
        val points = chartPoints(listOf(7, 7, 7), cellWidthPx = 64f, chartHeightPx = 96f, padPx = 16f)

        assertTrue(points.all { it.second == 48f })
    }

    @Test
    fun `tek ornek merkeze oturur`() {
        val points = chartPoints(listOf(10), cellWidthPx = 64f, chartHeightPx = 96f, padPx = 16f)

        assertEquals(1, points.size)
        assertEquals(32f, points[0].first, 0.01f)
        assertEquals(48f, points[0].second, 0.01f)
    }

    @Test
    fun `bos liste bos nokta uretir`() {
        assertTrue(chartPoints(emptyList(), cellWidthPx = 64f, chartHeightPx = 96f, padPx = 16f).isEmpty())
    }

    @Test
    fun `sekiz dilimlik yedek kaynak fiksturu n generic`() {
        val points = chartPoints(List(8) { it }, cellWidthPx = 64f, chartHeightPx = 96f, padPx = 16f)

        assertEquals(8, points.size)
        assertEquals(480f, points[7].first, 0.01f) // 7*64+32
    }

    @Test
    fun `yirmi dortluk birincil fikstur n generic`() {
        val points = chartPoints(List(24) { 15 + it % 10 }, cellWidthPx = 64f, chartHeightPx = 96f, padPx = 16f)

        assertEquals(24, points.size)
        assertEquals(1504f, points[23].first, 0.01f) // 23*64+32
    }
}

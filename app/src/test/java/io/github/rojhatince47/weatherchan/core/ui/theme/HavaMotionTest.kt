package io.github.rojhatince47.weatherchan.core.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HavaMotion şu anda hiçbir ekranda kullanılmıyor: F1'de temel atıldı, tüketicisi
 * F2 (mascot state machine geçişleri) ve F3 olacak. Kullanılmayan bir token
 * sessizce bozulmaya en yatkın şeydir, bu yüzden ölçek değişmez diye sabitleniyor.
 *
 * Bunlar karakterizasyon testleridir: kod bugün doğru, ama değerleri birisi
 * karıştırırsa yakalar.
 */
class HavaMotionTest {

    @Test
    fun `durations ascend with token importance`() {
        assertTrue(
            "FAST <= MEDIUM olmali",
            HavaMotion.FAST <= HavaMotion.MEDIUM
        )
        assertTrue(
            "MEDIUM <= SLOW olmali",
            HavaMotion.MEDIUM <= HavaMotion.SLOW
        )
    }

    @Test
    fun `durations stay in a range that feels responsive`() {
        // 100ms alti göz ucu flicks, 500ms ustu gecikmis hissettirir.
        val all = listOf(HavaMotion.FAST, HavaMotion.MEDIUM, HavaMotion.SLOW)
        all.forEach { duration ->
            assertTrue("$duration çok kısa", duration >= 100)
            assertTrue("$duration çok uzun", duration <= 500)
        }
    }

    @Test
    fun `durations are distinct`() {
        // Aynı değeri taşıyan iki token, biri için ayrıldığında diğeri de
        // değişmiş gibi görünür; ayrı kalmalılar.
        assertEquals(3, setOf(HavaMotion.FAST, HavaMotion.MEDIUM, HavaMotion.SLOW).size)
    }
}

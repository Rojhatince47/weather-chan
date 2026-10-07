package io.github.rojhatince47.weatherchan.feature.city.domain.suggestion

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import kotlinx.coroutines.delay

/**
 * Sağlayıcıyı testten yönetmek için sahte: çağrıları sayar, sonucu test belirler.
 *
 * - `result` → önbellek/ilk okuma (`nearby`).
 * - `refreshResult` → canlı tazeleme (`refresh`); verilmezse `result`'a düşer.
 * - `refreshDelayMs` → canlı turun asılı kalması (iki aşamalı yayımı gözlemlemek için).
 * - `nearbyDelayMs` → Yakınında aşamasının asılı kalması (dosya-önce aşamayı gözlemlemek için).
 * - `result` `null` → sağlayıcı "veri yok" (öncelik ülkeye düşer).
 */
class FakeNearbyCitySuggestionProvider(
    var result: List<CitySeed>? = null,
    var refreshResult: List<CitySeed>? = result,
    var nearbyDelayMs: Long = 0L,
    var refreshDelayMs: Long = 0L
) : NearbyCitySuggestionProvider {

    var refreshCalls: Int = 0
        private set
    val origins: MutableList<Coordinates> = mutableListOf()

    override suspend fun nearby(origin: Coordinates): List<CitySeed>? {
        if (nearbyDelayMs > 0) delay(nearbyDelayMs)
        origins += origin
        return result
    }

    override suspend fun refresh(origin: Coordinates): List<CitySeed>? {
        refreshCalls += 1
        if (refreshDelayMs > 0) delay(refreshDelayMs)
        origins += origin
        return refreshResult
    }
}

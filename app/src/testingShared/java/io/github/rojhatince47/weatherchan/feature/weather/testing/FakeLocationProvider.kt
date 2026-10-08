package io.github.rojhatince47.weatherchan.feature.weather.testing

import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import kotlinx.coroutines.CompletableDeferred

/** Sabit konum döndüren sahte; `null` verirsen konum okunamaz. */
class FakeLocationProvider(
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

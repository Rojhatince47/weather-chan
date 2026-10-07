package io.github.rojhatince47.weatherchan.feature.weather.domain.usecase

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CachedForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

class GetCachedForecastsUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    // `first()` DEĞİL `firstOrNull()`: hiç yayımlamadan biten akışta (`emptyFlow`)
    // `first()` `NoSuchElementException` fırlatır, `firstOrNull()` `null` döner.
    suspend operator fun invoke(): Map<Long, CachedForecast>? = repository.cachedForecasts().firstOrNull()
}

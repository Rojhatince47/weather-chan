package io.github.rojhatince47.weatherchan.feature.weather.domain.usecase

import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class SearchCitiesUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(query: String): AppResult<List<City>> = repository.search(query)
}

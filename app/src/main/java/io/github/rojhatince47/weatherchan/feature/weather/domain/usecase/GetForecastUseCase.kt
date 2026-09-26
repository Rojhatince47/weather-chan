package io.github.rojhatince47.weatherchan.feature.weather.domain.usecase

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class GetForecastUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    operator fun invoke(cityId: Long): Forecast = repository.forecast(cityId)
}

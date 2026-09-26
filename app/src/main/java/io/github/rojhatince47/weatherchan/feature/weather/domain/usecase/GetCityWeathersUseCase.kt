package io.github.rojhatince47.weatherchan.feature.weather.domain.usecase

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class GetCityWeathersUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    operator fun invoke(): List<CityWeather> = repository.cityWeathers()
}

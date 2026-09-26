package io.github.rojhatince47.weatherchan.feature.weather.data.repository

import io.github.rojhatince47.weatherchan.feature.weather.data.local.SampleWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val sampleDataSource: SampleWeatherDataSource
) : WeatherRepository {

    override fun cityWeathers(): List<CityWeather> = sampleDataSource.cityWeathers

    override fun forecast(cityId: Long): Forecast = sampleDataSource.forecast(cityId)
}

package io.github.rojhatince47.weatherchan.feature.weather.domain.repository

import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast

interface WeatherRepository {
    /**
     * [cities] listesi için hava durumu çeker. Şehir listesi çağırandan gelir;
     * repository bir şehir listesi tutmaz.
     */
    suspend fun cityWeathers(cities: List<City>): AppResult<List<CityWeather>>

    suspend fun forecast(city: City): AppResult<Forecast>

    /** Canlı şehir araması (Open-Meteo geocoding, yedekli). */
    suspend fun search(query: String): AppResult<List<City>>
}

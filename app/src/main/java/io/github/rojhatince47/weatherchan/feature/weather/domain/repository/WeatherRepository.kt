package io.github.rojhatince47.weatherchan.feature.weather.domain.repository

import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CachedForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    /**
     * [cities] listesi için hava durumu çeker. Şehir listesi çağırandan gelir;
     * repository bir şehir listesi tutmaz.
     */
    suspend fun cityWeathers(cities: List<City>): AppResult<List<CityWeather>>

    /** Şehir-başı son başarılı tahminler; `Long.MIN_VALUE` konum kaydıdır. Boş map = kayıt yok. */
    fun cachedForecasts(): Flow<Map<Long, CachedForecast>>

    /** Diskteki son çözülmüş konum adı; yoksa/bozuksa `null` akar. */
    fun cachedLocationCity(): Flow<City?>

    /**
     * Çözülmüş konum adını saklar.
     *
     * NEYİN saklanacağına çağıran karar verir ([GetLocationCityUseCase] yalnız
     * gerçek adı yazar); bu metot verileni aynen yazar.
     */
    suspend fun saveLocationCity(city: City)

    suspend fun forecast(city: City): AppResult<Forecast>

    /** Canlı şehir araması (Open-Meteo geocoding, yedekli). */
    suspend fun search(query: String): AppResult<List<City>>
}

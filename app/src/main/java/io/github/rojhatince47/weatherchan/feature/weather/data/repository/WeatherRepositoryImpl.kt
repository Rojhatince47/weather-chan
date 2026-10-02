package io.github.rojhatince47.weatherchan.feature.weather.data.repository

import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.data.local.WeatherCachePreferences
import io.github.rojhatince47.weatherchan.feature.weather.data.local.toCachedDto
import io.github.rojhatince47.weatherchan.feature.weather.data.local.toDomain
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo.OpenMeteoWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin.WttrInWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CachedWeathers
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeatherRepositoryImpl @Inject constructor(
    private val primary: OpenMeteoWeatherDataSource,
    private val backup: WttrInWeatherDataSource,
    private val cache: WeatherCachePreferences,
    private val clock: Clock
) : WeatherRepository {

    // `toDomain()` bilinmeyen enum adında fırlatır (`valueOf`); blob-düzeyi
    // `runCatching` burada kurulur. Bozuk kayıt → `null` → önbellek-yok davranışı.
    override fun cachedWeathers(): Flow<CachedWeathers?> = cache.cached().map { runCatching { it?.toDomain() }.getOrNull() }

    override suspend fun cityWeathers(cities: List<City>): AppResult<List<CityWeather>> {
        val result = when (val primaryResult = primary.cityWeathers(cities)) {
            is AppResult.Success -> primaryResult
            is AppResult.Failure -> backup.cityWeathers(cities)
        }
        // Yalnız tam kapsama yazılır: kısmi turda eksik şehir düşerdi.
        if (result is AppResult.Success &&
            result.data.map { it.city.id }.containsAll(cities.map { it.id })
        ) {
            cache.write(result.data.toCachedDto(clock.instant().toEpochMilli()))
        }
        return result
    }

    override suspend fun forecast(city: City): AppResult<Forecast> = when (val primaryResult = primary.forecast(city)) {
        is AppResult.Success -> primaryResult
        is AppResult.Failure -> backup.forecast(city)
    }

    /**
     * Arama iki kaynağı da dener.
     *
     * Yedeğe düşmek için yalnız `Failure` yeterli **değildir**: birincil
     * kaynak aramayı başarıyla yürütebilir ama **hiçbir şey bulamayabilir**.
     * Konum adı çözümlemesi tam olarak bu duruma dayanır — sorgu
     * `"41.01,28.98"` biçiminde bir koordinattır, Open-Meteo geocoding'i ise
     * *ad* arar ve boş liste döner (HTTP 200). Yalnız `Failure`'a bakılsaydı
     * wttr.in hiç çağrılmaz ve konum kartı ağ sağlıklı olsa bile sürekli
     * "Konum" adını gösterirdi.
     *
     * Bu yüzden kural: birincil kaynak **hem başarısız olursa hem de boş
     * dönerse** yedeğe düşülür; kim bir şey bulduysa onun cevabı döner.
     * Hiçbiri bulamazsa, iki kaynaktan **gerçekten tamamlanmış** olanın cevabı
     * döner — aranan yer yoksa "sonuç yok", ağ çalışmıyorsa hata mesajı.
     */
    override suspend fun search(query: String): AppResult<List<City>> {
        val primaryResult = primary.searchCities(query)
        if (primaryResult is AppResult.Success && primaryResult.data.isNotEmpty()) {
            return primaryResult
        }
        val backupResult = backup.searchCities(query)
        if (backupResult is AppResult.Success && backupResult.data.isNotEmpty()) {
            return backupResult
        }
        // Kim bir şey bulamadıysa artık önemli olan şey **ağın çalışıp
        // çalışmadığı**. Birincil kaynak tamamlandıysa (200 + boş) onun cevabı
        // döner: kullanıcı gerçekten var olmayan bir yer aramıştır ve hata
        // mesajı yanıltıcı olur. Birincil de hata verdiyse hata gösterilir.
        return if (primaryResult is AppResult.Failure) backupResult else primaryResult
    }
}

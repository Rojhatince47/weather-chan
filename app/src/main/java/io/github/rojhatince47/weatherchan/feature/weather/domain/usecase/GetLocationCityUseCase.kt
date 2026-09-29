package io.github.rojhatince47.weatherchan.feature.weather.domain.usecase

import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import java.util.Locale
import javax.inject.Inject

/**
 * Cihazın o anki konumundan bir [City] üretir.
 *
 * **İki kaynaklı, kırılmaz akış:**
 * 1. Cihaz konumunu oku. Okunamazsa `null` döner — akış durmaz, çağıran boş
 *    durum gösterebilir.
 * 2. Okunan koordinatı `"lat,lon"` biçiminde **coğrafi çözümleme** yapmayan
 *    ham sorgu olarak gönder. Ad, bölge ve ülke buradan gelir.
 * 3. Ad çözümlenemezse (ağ hatası, hız sınırı, boş sonuç) koordinatın kendisi
 *    kullanılır ve ad `"Konum"` olur.
 *
 * 3. adım olmadan konum kartı boş veya anlamsız bir isim gösterirdi. Ad
 * çözülemediğinde bile **koordinat doğrudur**, bu yüzden akış hiçbir koşulda
 * durmaz: kullanıcı en azından doğru yeri ve hava durumunu görür.
 *
 * Üçüncü adımda kullanılan `Locale.US` bilinçlidir: sorgu dize olarak
 * gönderildiği için ondalık ayırıcının nokta olması gerekir; aksi halde
 * Türkçe locale'de `41,01,28,98` gönderilir ve çözümleme bozulur.
 */
class GetLocationCityUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(): City? {
        val coordinates = locationProvider.currentCoordinates() ?: return null

        val resolved = when (val result = weatherRepository.search(coordinates.toQuery())) {
            is AppResult.Success -> result.data.firstOrNull()
            is AppResult.Failure -> null
        }

        return (resolved ?: City(name = FALLBACK_NAME, region = null, country = null, coordinates = coordinates))
            .copy(isLocation = true)
    }

    private fun Coordinates.toQuery() = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)

    private companion object {
        const val FALLBACK_NAME = "Konum"
    }
}

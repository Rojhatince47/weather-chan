package io.github.rojhatince47.weatherchan.feature.weather.domain.usecase

import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import java.util.Locale
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.first

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

        // Önbellek vuruşu: ağa çıkılmaz. Taze koordinat taşınır (kart kimliği
        // güncel kalır); kayıt değişmezi gereği `isLocation` hep `true`'dur,
        // yine de açıkça zorlanır.
        weatherRepository.cachedLocationCity().first()?.takeIf {
            distanceKm(it.coordinates, coordinates) <= NEARBY_KM
        }?.let {
            return it.copy(coordinates = coordinates, isLocation = true)
        }

        val query = coordinates.toQuery()
        val resolved = when (val result = weatherRepository.search(query)) {
            is AppResult.Success -> result.data.firstOrNull()
            is AppResult.Failure -> null
        }
        // wttr.in boş alanlarda fallback olarak sorgunun kendisini
        // (koordinat dizesi) döndürebilir; bu "ad" sayılmaz (spec §7).
        val areaName = resolved?.name?.takeIf { it.isNotBlank() && it != query }
        val region = resolved?.region?.takeIf { it.isNotBlank() }

        // Doğruluk > hassasiyet (spec §3): bölge adı yerleşim adına tercih
        // edilir. Koordinat daima GPS'tir; çözülen kaydın merkezi kullanılmaz.
        val city = City(
            name = region ?: areaName ?: "",
            region = region,
            country = resolved?.country,
            coordinates = coordinates,
            isLocation = true
        )
        // Önbelleğe yalnız gerçek, boş olmayan ad yazılır (spec §7/§10).
        if (city.name.isNotBlank()) {
            weatherRepository.saveLocationCity(city)
        }
        return city
    }

    private fun Coordinates.toQuery() = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)
}

/**
 * Önbellek hücresi yarıçapı (km). Mevcut `SuggestCitiesUseCase` formülünün
 * `Double` varyantıdır (`roundToInt` YOK — `Int` yuvarlama 0.9 ile 1.1'i
 * ayırt edemez).
 *
 * 2 km bilinçli tercihtir: durağan-hal GPS/ağ-sağlayıcı seğirtmesi (cihazda
 * ölçülen ~1.1 km atlamalar) eşiğin altında kalır; gerçekten yer değiştiren
 * kullanıcı (yürüyüşle 25+ dk, her sürüş) yeniden çözülür. Şehir-adı
 * doğruluğu için 2 km güvenlidir — yanlış ad penceresi yalnız 2 km+
 * harekettedir.
 */
private const val NEARBY_KM = 2.0

private fun distanceKm(from: Coordinates, to: Coordinates): Double {
    val earthRadiusKm = 6371.0
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val h = sin(dLat / 2) * sin(dLat / 2) + sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)
    return 2 * earthRadiusKm * asin(sqrt(min(1.0, h)))
}

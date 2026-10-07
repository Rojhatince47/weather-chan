package io.github.rojhatince47.weatherchan.feature.weather.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * İki şehrin "aynı yer" olup olmadığına mesafeyle karar verir.
 *
 * `City.id` koordinatın ~110 m yuvarlamasıdır; aynı yer GPS, geocoding,
 * gömülü liste ve Overpass'te farklı basamak hassasiyetiyle geldiği için
 * kimlik eşitliği kaynaklar arası mükerrer kayda izin verir (gerçek cihaz
 * verisi: konum "Mardin" ile arama "Derik" ~330 m arayla iki kart oldu).
 *
 * Eşik 10 km'dir: kaynak sapmaları (~5 km'e dek) kapsar; hava bölgesel
 * olduğundan yakın iki ayrı yerin karışması kabul edilir. Eşik Double
 * hassasiyetiyle karşılaştırılır — görüntüleme değeri (`Int` km) bununla
 * sınanamaz.
 */
object CityProximity {

    /** Aynı yer sayılma eşiği (km). */
    private const val SAME_PLACE_THRESHOLD_KM = 10.0

    fun isSamePlace(a: City, b: City): Boolean = distanceKm(a.coordinates, b.coordinates) < SAME_PLACE_THRESHOLD_KM

    /**
     * Haversine mesafesi (km) — yuvarlanmamış.
     *
     * `min(1.0, h)` sonuca değil `asin`'in argümanına uygulanır: kayan nokta
     * yuvarlaması `h`'yı 1'in üstüne taşıyabilir ve `asin(>1)` NaN üretir.
     * Görüntüleme yapan çağıran taraf `roundToInt()` uygular.
     */
    fun distanceKm(from: Coordinates, to: Coordinates): Double {
        val earthRadiusKm = 6371.0
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) + sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)
        return 2 * earthRadiusKm * asin(sqrt(min(1.0, h)))
    }
}

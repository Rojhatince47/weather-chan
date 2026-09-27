package io.github.rojhatince47.weatherchan.feature.city.data.local

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates

/**
 * İlk açılışta yazılan başlangıç listesi.
 *
 * Koordinatlar il merkezi koordinatlarıdır ve şehir düzeyi hava durumu için
 * yeterli hassasiyettedir. Yanlış koordinat yanlış hava demektir; bu yüzden
 * her biri canlı geocoding sonucuyla karşılaştırılarak doğrulanmıştır.
 */
object DefaultCitySeed {
    val cities: List<City> = listOf(
        city("İstanbul", "Marmara", 41.01, 28.98),
        city("Ankara", "İç Anadolu", 39.92, 32.85),
        city("İzmir", "Ege", 38.42, 27.14),
        city("Bursa", "Marmara", 40.19, 29.06),
        city("Antalya", "Akdeniz", 36.90, 30.70),
        city("Adana", "Akdeniz", 37.00, 35.32),
        city("Konya", "İç Anadolu", 37.87, 32.48),
        city("Gaziantep", "Güneydoğu Anadolu", 37.07, 37.38),
        city("Mersin", "Akdeniz", 36.81, 34.64),
        city("Kayseri", "İç Anadolu", 38.73, 35.49),
        city("Eskişehir", "Marmara", 39.77, 30.52),
        city("Diyarbakır", "Güneydoğu Anadolu", 37.91, 40.24),
        city("Samsun", "Karadeniz", 41.29, 36.33),
        city("Denizli", "Ege", 37.78, 29.09),
        city("Şanlıurfa", "Güneydoğu Anadolu", 37.16, 38.79),
        city("Malatya", "Doğu Anadolu", 38.35, 38.31),
        city("Erzurum", "Doğu Anadolu", 39.90, 41.27),
        city("Van", "Doğu Anadolu", 38.49, 43.38),
        city("Sivas", "İç Anadolu", 39.75, 37.02),
        city("Balıkesir", "Marmara", 39.65, 27.89),
        city("Manisa", "Ege", 38.61, 27.43),
        city("Aydın", "Ege", 37.85, 27.84),
        city("Muğla", "Ege", 37.22, 28.36),
        city("Kocaeli", "Marmara", 40.77, 29.92),
        city("Tekirdağ", "Marmara", 40.98, 27.51),
        city("Hatay", "Akdeniz", 36.20, 36.16),
        city("Kahramanmaraş", "Akdeniz", 37.58, 36.94),
        city("Ordu", "Karadeniz", 40.98, 37.88),
        city("Zonguldak", "Karadeniz", 41.45, 31.79),
        city("Elazığ", "Doğu Anadolu", 38.68, 39.22),
        city("Batman", "Güneydoğu Anadolu", 37.88, 41.13),
        city("Sakarya", "Marmara", 40.78, 30.40)
    )

    private fun city(name: String, region: String, latitude: Double, longitude: Double) = City(
        name = name,
        region = region,
        country = "Türkiye",
        coordinates = Coordinates(latitude = latitude, longitude = longitude)
    )
}

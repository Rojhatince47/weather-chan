package io.github.rojhatince47.weatherchan.feature.weather.domain.location

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates

/**
 * Cihazın o an bulunduğu coğrafi konumu.
 *
 * Aşırı soyut bir GPS arayüzü değil, ürünün ihtiyaç duyduğu en küçük yüzey:
 * "şu an nerede?" Tek bir metot, tek bir veri sınıfı.
 *
 * `LocationManager` üzerinden çalışan [AndroidLocationProvider] uygulaması
 * vardır. Bu arayüz sayesinde konum mantığı JUnit testlerinde gerçek cihaz
 * olmadan, sahte bir uygulama ile sınanabilir.
 */
fun interface LocationProvider {
    /**
     * Mevcut konumu döndürür.
     *
     * Dönüş değerleri:
     * - [Coordinates] → konum okundu.
     * - `null` → konum **şu anda** alınamadı: izin verilmemiş, GPS kapalı,
     *   sinyal yok veya cihaz konum servisini desteklemiyor.
     *
     * Yöntem **asla hata fırlatmaz**; okunamayan konum bir hata değil, veri
     * eksikliğidir. Çağıran taraf `null` gördüğünde kullanıcıya boş durum
     * gösterebilir, yeniden deneyebilir. Bu yüzden uygulama
     * [SecurityException] dahil tüm hataları yutar.
     */
    suspend fun currentCoordinates(): Coordinates?
}

package io.github.rojhatince47.weatherchan.feature.city.domain.repository

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import kotlinx.coroutines.flow.Flow

/**
 * Kullanıcının şehir listesinin tek gerçek kaynağı.
 */
interface CityRepository {
    /**
     * Depodaki liste. **İlk yayım zaten gerçek veridir** — akışta hiçbir zaman
     * "tahmini" ya da "geçici" bir başlangıç değeri bulunmaz.
     *
     * Bilerek `StateFlow` değil `Flow` seçildi: `StateFlow` bir **başlangıç
     * değeri** zorunlu kılar ve o değer diskteki gerçeği yansıtmadığı sürece
     * (ör. tohum, önbellek) soğuk açılışta yarış yaratır. `Flow` yalnız depodan
     * gelen değerleri yayar, dolayısıyla dinleyen ilk değer gerçeğin kendisidir.
     */
    val cities: Flow<List<City>>

    /**
     * Zaten listede olan şehir eklenirse mükerrer kayıt oluşmaz.
     *
     * Mutatörler `suspend` ve **yazılan listeyi** döndürür: çağıran, yazma bittikten
     * sonra elindeki liste neyse onunla devam eder. Yazma asenkrondur (DataStore),
     * bu yüzden dönen değer `cities` akışı okunmadan önce kullanılmalıdır;
     * aksi halde çağıran eski listeyi görüp yanlış veri çeker.
     */
    suspend fun add(city: City): List<City>

    suspend fun remove(cityId: Long): List<City>

    /**
     * Silinen şehri **eski sırasına** geri koyar.
     *
     * `add` bilinçli olarak sona ekler: yeni bir şehir listenin en sonuna
     * yerleşir. Geri al da `add` kullansaydı, kart ekranın en altında belirir
     * ve tüm liste yer değiştirirdi; kullanıcı hangi kartın neden taşındığını
     * anlamaz. Bu yüzden geri al kendi yolunu ayrı taşır.
     *
     * `index` silme anında ölçülmüş olabilir; arada başka şehirler silinmişse
     * liste kısalmış olur. Depo indisi güvenli şekilde sınırlar.
     */
    suspend fun restore(city: City, index: Int): List<City>

    /**
     * Şehri **konum olarak** ekler veya mevcut kaydı konuma yükseltir.
     *
     * `add` bilinçli olarak üzerine yazmaz: kullanıcı aynı şehri iki kez
     * aramadan ekleyemez. Konumda davranış farklıdır — kullanıcı şehri önce
     * aramadan eklemiş, sonra "Konumumu ekle" demiş olabilir. Bu durumda
     * mükerrer kayıt oluşturmak yerine **mevcut kayıt güncellenir**: ad,
     * bölge ve koordinat tazelenir, `isLocation` işaretlenir.
     *
     * Konum daima listenin başına alınır; diğer şehirlerin sırası korunur.
     */
    suspend fun addAsLocation(city: City): List<City>
}

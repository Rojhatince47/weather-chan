package io.github.rojhatince47.weatherchan.feature.city.testing

import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityProximity
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Test amaçlı şehir deposu. Varsayılan listesi eski örnek verenin 5 şehridir;
 * mevcut testlerin liste boyutu varsayımları korunur.
 *
 * **Sınırlama:** `cities` bir `StateFlow` olduğu için aynı `id`'li bir kaydın
 * **değiştirilmesi** gözlemlenemez (`City.equals` yalnız `id`'ye bakar, akış da
 * eşit değeri conflate eder). Gerçek depoda aynı id'li kayıt değiştirilir; bu
 * yüzden kimlik değişikliğini sınayan testler **farklı** bir koordinat
 * vermek zorundadır.
 *
 * `cities` bilinçli olarak `StateFlow` **değil** `Flow` olarak sunulur: gerçek
 * uygulamada depo `Flow` yaylar ve dinleyen ilk değeri gerçek veridir. Sahte de
 * aynı sözleşmeyi korur, böylece testler "ilk yayım doğru listeden başlar"
 * davranışını gerçekle üzerinde sınar. (Eski `staleStartupValue` ile
 * taklit edilen soğuk açılış yarışı artık mümkün değildir ve kaldırıldı.)
 *
 * Mükerrer kontrol gerçek depo gibi **yakınlıkla** yapılır (`CityProximity`,
 * 10 km): aynı yer farklı kaynak koordinatlarıyla gelirse tek kayıttır.
 */
class FakeCityRepository(
    initial: List<City> = listOf(
        City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98)),
        City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85)),
        City("İzmir", "Ege", "Türkiye", Coordinates(38.42, 27.14)),
        City("Antalya", "Akdeniz", "Türkiye", Coordinates(36.90, 32.70)),
        City("Trabzon", "Karadeniz", "Türkiye", Coordinates(41.00, 39.72))
    )
) : CityRepository {
    private val state = MutableStateFlow(initial)

    override val cities: Flow<List<City>> = state

    /** Yalnız test doğrulaması için: depodaki güncel liste. */
    fun current(): List<City> = state.value

    override suspend fun add(city: City): List<City> {
        val updated = if (state.value.any { CityProximity.isSamePlace(it, city) }) state.value else state.value + city
        state.value = updated
        return updated
    }

    override suspend fun remove(cityId: Long): List<City> {
        val updated = state.value.filterNot { it.id == cityId }
        state.value = updated
        return updated
    }

    override suspend fun restore(city: City, index: Int): List<City> {
        val current = state.value
        val updated = if (current.any { CityProximity.isSamePlace(it, city) }) {
            current
        } else {
            current.toMutableList().apply { add(index.coerceIn(0, current.size), city) }
        }
        state.value = updated
        return updated
    }

    override suspend fun addAsLocation(city: City): List<City> {
        // Gerçek depo sözleşmesi: konum tektir, mükerrer kontrol yakınlıkladır;
        // isimsiz konum kaydı aynı-yer isimli kartın adını korur.
        val sibling = state.value.firstOrNull { !it.isLocation && CityProximity.isSamePlace(it, city) }
        val location = city.copy(
            isLocation = true,
            name = city.name.ifBlank { sibling?.name.orEmpty() },
            region = city.region ?: sibling?.region,
            country = city.country ?: sibling?.country
        )
        val updated = listOf(location) + state.value.filterNot { it.isLocation || CityProximity.isSamePlace(it, location) }
        state.value = updated
        return updated
    }
}

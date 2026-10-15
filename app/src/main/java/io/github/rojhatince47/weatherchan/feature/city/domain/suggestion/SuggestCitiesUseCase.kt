package io.github.rojhatince47.weatherchan.feature.city.domain.suggestion

import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.ReverseGeocoder
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityProximity
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

/**
 * Kullanıcıya konum, ülke ya da dünya temelinde şehir önerir.
 *
 * Öncelik zinciri (ilk bulunan dal kazanır):
 * 1. **Yakınında** — çözülmüş konum varsa, mesafeye göre en yakın N.
 * 2. **Ülkende** — geri çözümlemenin ISO ülke koduna göre dosya sırası;
 *    konum yoksa son bilinen konum, o da yoksa cihaz ülke kodu.
 * 3. **Dünyada** — ülke kodu bilinmiyorsa ya da havuzda o ülkeden şehir
 *    yoksa dünya yedek sırası.
 *
 * Süzme **ISO-2 koda** göredir (geocoding'in `countryCode` alanı); ülke adı
 * eşleştirmesi yapılmaz — ad varyantları kırılgandı.
 *
 * **Konum izni asla bu akışta istenmez.** [LocationProvider] izni okumaz,
 * yalnız okumayı dener; izin yoksa `null` döner ve akış ülkeye düşer.
 *
 * Eleme bu akışta yapılmaz: "ekli" işareti ve öneri filtresi VM katmanındadır,
 * yakınlık eşleşmesiyle (spec: yakinlik eslesmesi ve oneri elemesi).
 */
class SuggestCitiesUseCase @Inject constructor(
    private val suggestionProvider: CitySuggestionProvider,
    private val nearbyProvider: NearbyCitySuggestionProvider,
    private val locationProvider: LocationProvider,
    private val deviceCountryProvider: DeviceCountryProvider,
    private val reverseGeocoder: ReverseGeocoder? = null,
    private val weatherRepository: WeatherRepository? = null
) {

    suspend operator fun invoke(limit: Int = DEFAULT_LIMIT, refresh: Boolean = false, nearbyEnabled: Boolean = true): SuggestionGroup? {
        val data = suggestionProvider.load()
        val pool = data?.cities?.filter { it.name.isNotBlank() && it.countryCode.length == 2 }.orEmpty()
        // `nearbyEnabled = false` yalnız dosya aşaması içindir: konum okunmaz
        // (12 sn GPS beklemesi dosya önerisini geciktirmesin), Yakınında
        // sağlamaya hiç girilmez — Ö2 aşamalı yayım sözleşmesi.
        val origin = if (nearbyEnabled) locationProvider.currentCoordinates() else null

        if (origin != null) {
            val nearby = if (refresh) nearbyProvider.refresh(origin) else nearbyProvider.nearby(origin)
            if (!nearby.isNullOrEmpty()) {
                return SuggestionGroup(
                    kind = SuggestionKind.Nearby,
                    suggestions = nearby
                        .map { CitySuggestion(it.toCity(), CityProximity.distanceKm(origin, it.coordinates).roundToInt()) }
                        .sortedBy { it.distanceKm!! }
                        .take(limit)
                )
            }
        }

        return groupForCountryWorld(
            countryCode = locationCountryCode(origin),
            pool = pool,
            worldFallbackOrder = data?.worldFallbackOrder.orEmpty(),
            limit = limit
        )
    }

    /**
     * "Ülkende" kovasının ülke kodu. Kaynak sırası: **asıl konum** (GPS'li
     * aşamada origin, dosya aşamasında son bilinen konum kaydı) üzerinden
     * geri çözümleme (ISO-2) → cihaz kodu. Cihaz yapılandırma locale'i
     * seyahatte/sahte GPS'te değişmediği için tek başına yeterli değildir.
     */
    private suspend fun locationCountryCode(origin: Coordinates?): String? {
        val coordinates = origin ?: weatherRepository?.cachedLocationCity()?.first()?.coordinates
        val resolved = coordinates?.let { reverseGeocoder?.reverse(it)?.countryCode }
            ?.takeIf { it.length == 2 }
        return resolved ?: deviceCountryProvider.countryCode()?.takeIf { it.length == 2 }
    }

    private fun groupForCountryWorld(
        countryCode: String?,
        pool: List<CitySeed>,
        worldFallbackOrder: List<String>,
        limit: Int
    ): SuggestionGroup? {
        val code = countryCode?.takeIf { it.length == 2 }
        if (code != null) {
            val inCountry = pool.filter { it.countryCode.equals(code, ignoreCase = true) }.take(limit)
            if (inCountry.isNotEmpty()) {
                return SuggestionGroup(SuggestionKind.Country, inCountry.map { CitySuggestion(it.toCity(), null) })
            }
        }
        val world = worldFallbackOrder
            .flatMap { fallbackCode -> pool.filter { it.countryCode == fallbackCode }.take(1) }
            .take(limit)
        return if (world.isEmpty()) {
            null
        } else {
            SuggestionGroup(SuggestionKind.World, world.map { CitySuggestion(it.toCity(), null) })
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 8
    }
}

package io.github.rojhatince47.weatherchan.feature.city.domain.suggestion

import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityProximity
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Kullanıcıya konum, ülke ya da dünya temelinde şehir önerir.
 *
 * Öncelik zinciri (ilk bulunan dal kazanır):
 * 1. **Yakınında** — çözülmüş konum varsa, mesafeye göre en yakın N.
 * 2. **Ülkende** — konum yoksa, cihaz ülke kodu verideyse dosya sırası.
 * 3. **Dünyada** — ne konum ne ülke biliniyorsa, dünya yedek sırası.
 *
 * Ülkeye göre **süzme yapılmaz**: geocoding ülke *adı* döndürür, kodu değil;
 * ada göre eşleştirme kırılgan olurdu. En yakın N şehir zaten kullanıcının
 * ülkesindedir.
 *
 * **Konum izni asla bu akışta istenmez.** [LocationProvider] izni okumaz,
 * yalnız okumayı dener; izin yoksa `null` döner ve akış ülkeye düşer.
 *
 * Eleme yapılmaz: "ekli" işareti VM/UI katmanındadır (spec: tik donutu).
 */
class SuggestCitiesUseCase @Inject constructor(
    private val suggestionProvider: CitySuggestionProvider,
    private val nearbyProvider: NearbyCitySuggestionProvider,
    private val locationProvider: LocationProvider,
    private val deviceCountryProvider: DeviceCountryProvider
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
            countryCode = origin?.let { deviceCountryProvider.countryCode() },
            pool = pool,
            worldFallbackOrder = data?.worldFallbackOrder.orEmpty(),
            limit = limit
        )
    }

    private fun groupForCountryWorld(
        countryCode: String?,
        pool: List<CitySeed>,
        worldFallbackOrder: List<String>,
        limit: Int
    ): SuggestionGroup? {
        val code = countryCode?.takeIf { it.length == 2 } ?: deviceCountryProvider.countryCode()?.takeIf { it.length == 2 }
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

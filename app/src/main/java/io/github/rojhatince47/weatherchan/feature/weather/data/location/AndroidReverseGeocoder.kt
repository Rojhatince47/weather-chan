package io.github.rojhatince47.weatherchan.feature.weather.data.location

import android.content.Context
import android.location.Geocoder
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.PlaceName
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.ReverseGeocoder
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [Geocoder] tabanlı [ReverseGeocoder] uygulaması. Uygulama dilinde ad döner
 * (wttr.in zincirinin dil/yazım sorunları burada giderilir).
 *
 * **Neden framework Geocoder:** yeni bağımlılık getirmez (Play Services
 * zorunluluğu yok); GMS'siz cihazda boş döner ve çağıran zincir eski
 * arama-yöntemine düşer. Hata asla dışarı sızmaz: çözülemezse `null`.
 */
@Singleton
class AndroidReverseGeocoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val languageProvider: () -> String
) : ReverseGeocoder {

    override suspend fun reverse(coordinates: Coordinates): PlaceName? = withContext(Dispatchers.IO) {
        runCatching {
            val locale = Locale.forLanguageTag(languageProvider())
            val geocoder = Geocoder(context, locale)

            @Suppress("DEPRECATION")
            val address = geocoder
                .getFromLocation(coordinates.latitude, coordinates.longitude, 1)
                ?.firstOrNull() ?: return@runCatching null
            val name = address.locality ?: address.subAdminArea ?: address.adminArea ?: return@runCatching null
            PlaceName(
                name = name,
                region = address.adminArea,
                country = address.countryName,
                countryCode = address.countryCode?.uppercase()
            )
        }.getOrNull()
    }
}

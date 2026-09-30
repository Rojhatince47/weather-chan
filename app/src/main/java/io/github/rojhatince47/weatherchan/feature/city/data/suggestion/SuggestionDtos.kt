package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySeed
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionData
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Gömülü öneri verisi dosyasının şeması.
 *
 * Alanların hiçbirinde varsayılan değer yoktur: gömülü dosyada eski kayıt ya
 * da ileri uyumluluk söz konusu değildir (sürüm geçişi `version` alanıyla
 * taşınır). Eksik alan sessizce varsayılana düşseydi, örn. `lat` yazılmayan
 * bir şehir (0, 0)'a — Körfez'e — taşınır ve bütünlük testi yeşil kalırdı.
 * Varsayılanın olmaması eksik alanı decode hatasına çevirir; hata hem
 * bütünlük testinde görünür hem de parser'da `null`'a düşer.
 */
@Serializable
internal data class SuggestionFileDto(
    @SerialName("version") val version: Int,
    @SerialName("worldFallbackOrder") val worldFallbackOrder: List<String>,
    @SerialName("cities") val cities: List<CityDto>
)

@Serializable
internal data class CityDto(
    @SerialName("name") val name: String,
    @SerialName("code") val code: String,
    @SerialName("country") val country: String,
    @SerialName("lat") val lat: Double,
    @SerialName("lon") val lon: Double
)

/**
 * Gömülü veri dosyasının **saf** okuyucusu.
 *
 * Android'e bağımlı olmayan tek parça: [AssetCitySuggestionProvider] yalnızca
 * dosyayı okur ve buraya verir. Böylece bozuk dosya, sürüm uyuşmazlığı ve alan
 * eşlemesi JUnit testlerinde sınanabilir.
 *
 * Ayrıştırma **asla istisna fırlatmaz**: bozuk veri bir hatadır ama kullanıcıya
 * gösterecek bir hata değil — öneri bölümü çizilmez, arama çalışmaya devam eder.
 */
internal object SuggestionFileParser {

    const val SUPPORTED_VERSION = 1

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String?): SuggestionData? {
        if (raw.isNullOrBlank()) return null
        val dto = runCatching { json.decodeFromString<SuggestionFileDto>(raw) }.getOrNull() ?: return null
        if (dto.version != SUPPORTED_VERSION) return null
        return SuggestionData(
            cities = dto.cities
                .filter { it.name.isNotBlank() && it.code.length == 2 }
                .map {
                    CitySeed(
                        name = it.name,
                        countryCode = it.code,
                        countryName = it.country,
                        coordinates = Coordinates(it.lat, it.lon)
                    )
                },
            worldFallbackOrder = dto.worldFallbackOrder.filter { it.length == 2 }
        )
    }
}

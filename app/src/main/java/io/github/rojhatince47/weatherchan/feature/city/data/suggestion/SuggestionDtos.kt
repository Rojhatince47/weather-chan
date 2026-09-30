package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

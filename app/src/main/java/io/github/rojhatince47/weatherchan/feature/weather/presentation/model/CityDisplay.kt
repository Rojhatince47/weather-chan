package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

/**
 * Konum kartının görünen tam adı: yerleşim + bölge birleştirilir
 * ("Derik, Mardin"). Yalnız biri varsa tek; aynıysa tekrar edilmez;
 * ikisi de yoksa sonuç boş (UI `location_default_name` çözer).
 * Normal şehirlerde `name` aynen döner.
 */
fun displayTitle(name: String?, region: String?, isLocation: Boolean): String {
    if (!isLocation) return name.orEmpty()
    val parts = listOfNotNull(
        name?.takeIf { it.isNotBlank() },
        region?.takeIf { it.isNotBlank() && it != name?.trim() }
    ).distinct()
    return parts.joinToString(", ")
}

package io.github.rojhatince47.weatherchan.feature.city.domain.suggestion

/** Gömülü öneri verisinin tamamı. */
data class SuggestionData(
    val cities: List<CitySeed>,
    /** Dünya dalında hangi ülkeden kaç şehir alınacağını belirler. */
    val worldFallbackOrder: List<String>
)

/**
 * Öneri verisinin kaynağı. Arayüz sayesinde motor, verinin nereden geldiğini
 * bilmez: gömülü dosya yerine testte sahte liste verilebilir.
 */
fun interface CitySuggestionProvider {
    /** Veri yoksa ya da okunamıyorsa `null` — öneri bölümü çizilmez. */
    suspend fun load(): SuggestionData?
}

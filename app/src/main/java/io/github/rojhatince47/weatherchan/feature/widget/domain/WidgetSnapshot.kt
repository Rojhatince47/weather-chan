package io.github.rojhatince47.weatherchan.feature.widget.domain

/**
 * Widget'ın çizim için ihtiyaç duyduğu tek anlık görüntü.
 *
 * Enum'lar ad olarak saklanır: widget süreci uygulama sürümünden bağımsız
 * okur; bilinmeyen bir ad çizimde güvenli varsayılana düşer.
 */
data class WidgetSnapshot(
    val cityName: String,
    val temperatureCelsius: Int,
    val unit: String,
    val condition: String,
    val isDay: Boolean,
    val pose: String,
    val hourly: List<WidgetHourSnapshot> = emptyList(),
    val updatedAtEpochMillis: Long,
    val adviceCategory: String = "CLEAR",
    val adviceSeed: Int = 0
)

data class WidgetHourSnapshot(
    val hourOfDay: Int,
    val temperatureCelsius: Int,
    val condition: String
)

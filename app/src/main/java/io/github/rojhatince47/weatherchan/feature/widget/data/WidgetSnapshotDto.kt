package io.github.rojhatince47.weatherchan.feature.widget.data

import io.github.rojhatince47.weatherchan.feature.widget.domain.WidgetHourSnapshot
import io.github.rojhatince47.weatherchan.feature.widget.domain.WidgetSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Disk biçimi; domain modeli framework'e bağımlı kalmayacak şekilde ayrıdır. */
@Serializable
internal data class WidgetSnapshotDto(
    @SerialName("cityName") val cityName: String,
    @SerialName("temperatureCelsius") val temperatureCelsius: Int,
    @SerialName("unit") val unit: String,
    @SerialName("condition") val condition: String,
    @SerialName("isDay") val isDay: Boolean,
    @SerialName("pose") val pose: String,
    @SerialName("hourly") val hourly: List<WidgetHourDto> = emptyList(),
    @SerialName("updatedAtEpochMillis") val updatedAtEpochMillis: Long,
    @SerialName("adviceCategory") val adviceCategory: String = "CLEAR",
    @SerialName("adviceSeed") val adviceSeed: Int = 0
)

@Serializable
internal data class WidgetHourDto(
    @SerialName("hourOfDay") val hourOfDay: Int,
    @SerialName("temperatureCelsius") val temperatureCelsius: Int,
    @SerialName("condition") val condition: String
)

internal fun WidgetSnapshot.toDto(): WidgetSnapshotDto = WidgetSnapshotDto(
    cityName = cityName,
    temperatureCelsius = temperatureCelsius,
    unit = unit,
    condition = condition,
    isDay = isDay,
    pose = pose,
    hourly = hourly.map { WidgetHourDto(it.hourOfDay, it.temperatureCelsius, it.condition) },
    updatedAtEpochMillis = updatedAtEpochMillis,
    adviceCategory = adviceCategory,
    adviceSeed = adviceSeed
)

internal fun WidgetSnapshotDto.toDomain(): WidgetSnapshot = WidgetSnapshot(
    cityName = cityName,
    temperatureCelsius = temperatureCelsius,
    unit = unit,
    condition = condition,
    isDay = isDay,
    pose = pose,
    hourly = hourly.map { WidgetHourSnapshot(it.hourOfDay, it.temperatureCelsius, it.condition) },
    updatedAtEpochMillis = updatedAtEpochMillis,
    adviceCategory = adviceCategory,
    adviceSeed = adviceSeed
)

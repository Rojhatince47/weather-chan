package io.github.rojhatince47.weatherchan.feature.widget.domain

/** Widget anlık görüntüsünün okuma/yazma sözleşmesi. */
interface WidgetSnapshotRepository {
    suspend fun read(): WidgetSnapshot?

    suspend fun write(snapshot: WidgetSnapshot)
}

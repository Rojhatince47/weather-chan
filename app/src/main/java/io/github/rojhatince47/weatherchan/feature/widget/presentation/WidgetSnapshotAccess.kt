package io.github.rojhatince47.weatherchan.feature.widget.presentation

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import io.github.rojhatince47.weatherchan.feature.widget.di.WidgetEntryPoint
import io.github.rojhatince47.weatherchan.feature.widget.domain.WidgetSnapshot

/**
 * Widget çizimi için anlık görüntüyü güvenle okur. Hilt/DataStore henüz
 * hazır değilse veya kayıt bozuksa launcher'ın "widget yüklenmiyor" ekranına
 * düşmek yerine boş durum gösterilir.
 */
internal suspend fun readWidgetSnapshot(context: Context): WidgetSnapshot? = runCatching {
    EntryPointAccessors
        .fromApplication(context, WidgetEntryPoint::class.java)
        .snapshotStore()
        .read()
}.getOrNull()

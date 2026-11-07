package io.github.rojhatince47.weatherchan.feature.widget.presentation

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import dagger.hilt.android.EntryPointAccessors
import io.github.rojhatince47.weatherchan.feature.widget.di.WidgetEntryPoint
import io.github.rojhatince47.weatherchan.feature.widget.worker.WidgetScheduler

class WeatherChatWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = EntryPointAccessors
            .fromApplication(context, WidgetEntryPoint::class.java)
            .snapshotStore()
            .read()
        provideContent { ChatWidgetContent(snapshot) }
    }
}

class WeatherChatWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = WeatherChatWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetScheduler.ensurePeriodic(context)
        WidgetScheduler.enqueueImmediate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetScheduler.cancelIfUnused(context, hasWidgets(context))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetScheduler.ensurePeriodic(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // Yerleşim/boyut kesinleşince taze veri iste: widget ilk eklendiğinde
        // "veri yok" görünüp bir sonraki 30 dakikaya kalmasın.
        WidgetScheduler.enqueueImmediate(context)
    }
}

/** İki widget türünden en az biri ana ekranda mı? */
internal fun hasWidgets(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(
        ComponentName(context, WeatherChatWidgetReceiver::class.java)
    ) + manager.getAppWidgetIds(
        ComponentName(context, WeatherInfoWidgetReceiver::class.java)
    ) + manager.getAppWidgetIds(
        ComponentName(context, WeatherCharacterWidgetReceiver::class.java)
    )
    return ids.isNotEmpty()
}

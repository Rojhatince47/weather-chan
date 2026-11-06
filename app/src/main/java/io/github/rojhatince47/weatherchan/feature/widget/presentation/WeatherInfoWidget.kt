package io.github.rojhatince47.weatherchan.feature.widget.presentation

import android.appwidget.AppWidgetManager
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

class WeatherInfoWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = EntryPointAccessors
            .fromApplication(context, WidgetEntryPoint::class.java)
            .snapshotStore()
            .read()
        provideContent { InfoWidgetContent(snapshot) }
    }
}

class WeatherInfoWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = WeatherInfoWidget()

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
        WidgetScheduler.enqueueImmediate(context)
    }
}

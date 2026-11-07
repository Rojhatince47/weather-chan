package io.github.rojhatince47.weatherchan.feature.widget.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.rojhatince47.weatherchan.feature.widget.worker.WidgetScheduler

/**
 * Uygulama güncellenince widget'ı hemen tazele. Aksi halde güncelleme
 * sırasında ölen süreç yüzünden launcher bir süre "widget yüklenmiyor"
 * gösterebiliyor.
 */
class WidgetPackageReplacedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (!hasWidgets(context)) return
        WidgetScheduler.ensurePeriodic(context)
        WidgetScheduler.enqueueImmediate(context)
    }
}

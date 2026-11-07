package io.github.rojhatince47.weatherchan.feature.widget.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.rojhatince47.weatherchan.feature.widget.presentation.WeatherCharacterWidget
import io.github.rojhatince47.weatherchan.feature.widget.presentation.WeatherChatWidget
import io.github.rojhatince47.weatherchan.feature.widget.presentation.WeatherInfoWidget
import io.github.rojhatince47.weatherchan.feature.widget.presentation.WeatherTalkingWidget
import javax.inject.Inject

/** Worker veriyi yazdıktan sonra iki widget'ı birlikte tazeler. */
class GlanceWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetRefresher {

    override suspend fun refresh() {
        WeatherChatWidget().updateAll(context)
        WeatherInfoWidget().updateAll(context)
        WeatherCharacterWidget().updateAll(context)
        WeatherTalkingWidget().updateAll(context)
    }
}

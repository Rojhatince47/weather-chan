package io.github.rojhatince47.weatherchan.feature.weather.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.rojhatince47.weatherchan.core.ui.theme.DecorFamily
import io.github.rojhatince47.weatherchan.core.ui.theme.DecorTones
import io.github.rojhatince47.weatherchan.core.ui.theme.WeatherBackgroundPalette

/**
 * Hava durumuna göre arka plan: dikey gradyan + dekoratif sahne.
 * Palet çağırandan gelir (K10); koşul görmez. `animatorsEnabled == false`
 * ise sahne statiktir (K8) — animasyonlar sonraki adımda eklenir.
 */
@Composable
fun WeatherBackground(
    palette: WeatherBackgroundPalette,
    animatorsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(palette.gradient))
        }
        val decor = palette.decor
        if (decor != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawDecor(
                    layout = decorLayout(palette.family, this.size, palette.isDay),
                    tones = decor,
                    size = this.size,
                    family = palette.family,
                    isDay = palette.isDay
                )
            }
        }
    }
}

internal fun DrawScope.drawDecor(
    layout: DecorLayout,
    tones: DecorTones,
    size: Size,
    family: DecorFamily,
    isDay: Boolean
) {
    val unit = size.minDimension

    fun at(p: Offset): Offset = Offset(p.x * size.width, p.y * size.height)

    // Yıldızlar
    layout.stars.forEach { p ->
        drawCircle(color = tones.star, radius = 0.006f * unit, center = at(p))
    }

    // Gök cismi: gündüz dolu disk (güneş), gece hilal (PathOperation.Difference).
    // Z-sırası: bulutların ÖNCE çizilir ki PARTLY_CLOUDY'de bulutlar örtsün (spec §3.2).
    layout.celestial?.let { p ->
        val c = at(p)
        val r = 0.11f * unit
        drawCircle(color = tones.glow, radius = r * 1.8f, center = c)
        if (isDay) {
            drawCircle(color = tones.body, radius = r, center = c)
        } else {
            val outer = Path().apply {
                addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r))
            }
            val inner = Path().apply {
                addOval(Rect(c.x - r * 0.62f, c.y - r * 1.08f, c.x + r * 1.38f, c.y + r * 0.92f))
            }
            drawPath(Path.combine(PathOperation.Difference, outer, inner), color = tones.body)
        }
    }

    // Bulutlar (kenar + dolgu; ConditionIcons.kt kompozisyonunun sadeleşmişi)
    layout.clouds.forEachIndexed { i, p ->
        drawCloud(tones, at(p), unit * (0.16f + 0.02f * i))
    }

    // Damlalar / kar taneleri (statik: sabit konum)
    layout.streaks.forEach { p ->
        val c = at(p)
        if (family == DecorFamily.SNOW) {
            drawCircle(color = tones.streak, radius = 0.010f * unit, center = c)
        } else {
            drawRoundRect(
                color = tones.streak,
                topLeft = Offset(c.x - 0.004f * unit, c.y - 0.022f * unit),
                size = Size(0.008f * unit, 0.044f * unit),
                cornerRadius = CornerRadius(0.004f * unit)
            )
        }
    }
}

internal fun DrawScope.drawCloud(tones: DecorTones, c: Offset, s: Float) {
    val circles = listOf(
        Triple(c.x - 0.5f * s, c.y, 0.28f * s),
        Triple(c.x, c.y - 0.12f * s, 0.36f * s),
        Triple(c.x + 0.5f * s, c.y, 0.24f * s)
    )
    val edge = 0.004f * s * 25f
    circles.forEach { (cx, cy, r) ->
        drawCircle(tones.cloudEdge, radius = r + edge, center = Offset(cx, cy))
    }
    drawRoundRect(
        color = tones.cloudEdge,
        topLeft = Offset(c.x - 0.78f * s - edge, c.y - 0.02f * s),
        size = Size(1.56f * s + 2 * edge, 0.34f * s + edge),
        cornerRadius = CornerRadius(0.12f * s)
    )
    circles.forEach { (cx, cy, r) ->
        drawCircle(tones.cloudFill, radius = r, center = Offset(cx, cy))
    }
    drawRoundRect(
        color = tones.cloudFill,
        topLeft = Offset(c.x - 0.78f * s, c.y - 0.02f * s),
        size = Size(1.56f * s, 0.34f * s),
        cornerRadius = CornerRadius(0.12f * s)
    )
}
package io.github.rojhatince47.weatherchan.feature.weather.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.rojhatince47.weatherchan.core.ui.theme.DecorFamily
import io.github.rojhatince47.weatherchan.core.ui.theme.DecorTones
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaMotion
import io.github.rojhatince47.weatherchan.core.ui.theme.WeatherAmbience
import io.github.rojhatince47.weatherchan.core.ui.theme.WeatherBackgroundPalette
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Hava durumuna göre arka plan: dikey gradyan + dekoratif sahne + ambiyans.
 * Palet çağırandan gelir (K10); koşul görmez.
 *
 * `animatorsEnabled == false` yalnız ambiyansı kapatır (K8); renk/dekor
 * geçişleri (`animateColorAsState`/`Crossfade`, HavaMotion.SLOW) iki modda da
 * yumuşaktır.
 */
@Composable
fun WeatherBackground(palette: WeatherBackgroundPalette, animatorsEnabled: Boolean, modifier: Modifier = Modifier) {
    val animatedStops = palette.gradient.map { stop ->
        animateColorAsState(targetValue = stop, animationSpec = tween(HavaMotion.SLOW), label = "bgStop")
    }
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            // Duraklar draw fazında okunur: invalidasyon draw'a sıkışar.
            drawRect(Brush.verticalGradient(animatedStops.map { it.value }))
        }
        val decor = palette.decor
        if (decor != null) {
            Crossfade(targetState = decor, animationSpec = tween(HavaMotion.SLOW), label = "bgDecor") { tones ->
                WeatherDecor(
                    family = palette.family,
                    isDay = palette.isDay,
                    tones = tones,
                    animatorsEnabled = animatorsEnabled
                )
            }
        }
    }
}

@Composable
private fun WeatherDecor(family: DecorFamily, isDay: Boolean, tones: DecorTones, animatorsEnabled: Boolean) {
    val transition = rememberInfiniteTransition(label = "ambience")

    // animateFloat composable'dır; ancak gövdeden çağrılır. `by` delegate +
    // değeri yalnız draw bloğunda okumak: her karede recomposition olmaz.
    fun animSpec(durationMs: Int) = infiniteRepeatable<Float>(
        animation = tween(durationMs, easing = LinearEasing)
    )
    val cloudPhase by if (animatorsEnabled) {
        transition.animateFloat(0f, 1f, animSpec(WeatherAmbience.CLOUD_DRIFT_MID_MS), label = "cloud")
    } else {
        remember { mutableStateOf(0f) }
    }
    val fallPhase by if (animatorsEnabled) {
        transition.animateFloat(
            0f,
            1f,
            animSpec(if (family == DecorFamily.SNOW) WeatherAmbience.SNOW_FALL_MS else WeatherAmbience.RAIN_FALL_MS),
            label = "fall"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    val twinklePhase by if (animatorsEnabled) {
        transition.animateFloat(0f, 1f, animSpec(WeatherAmbience.TWINKLE_MS), label = "twinkle")
    } else {
        remember { mutableStateOf(0f) }
    }

    // Şimşek: nadir parlama (yalnız THUNDERSTORM tonları + animatorsEnabled).
    val flashAlpha = remember { Animatable(0f) }
    if (animatorsEnabled && tones.flash != Color.Transparent) {
        LaunchedEffect(Unit) {
            val rng = Random(42)
            while (true) {
                delay(rng.nextLong(WeatherAmbience.LIGHTNING_MIN_MS.toLong(), WeatherAmbience.LIGHTNING_MAX_MS.toLong()))
                flashAlpha.animateTo(1f, tween(60))
                flashAlpha.animateTo(0f, tween(320))
            }
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val layout = decorLayout(family, this.size, isDay)
        val unit = this.size.minDimension

        fun at(x: Float, y: Float) = Offset(x * this.size.width, y * this.size.height)

        // Yıldızlar (fazlı titreme)
        layout.stars.forEachIndexed { i, p ->
            val a = if (animatorsEnabled) {
                (0.55f + 0.45f * sin((twinklePhase + i * 0.17f) * 2f * Math.PI.toFloat())).coerceIn(0f, 1f)
            } else {
                1f
            }
            drawCircle(color = tones.star.copy(alpha = tones.star.alpha * a), radius = 0.006f * unit, center = at(p.x, p.y))
        }

        // Gök cismi bulutların ÖNCE (arkada) çizilir — PARTLY_CLOUDY'de örtülme.
        layout.celestial?.let { p ->
            val c = at(p.x, p.y)
            val r = 0.11f * unit
            drawCircle(color = tones.glow, radius = r * 1.8f, center = c)
            if (isDay) {
                drawCircle(color = tones.body, radius = r, center = c)
            } else {
                val outer = Path().apply { addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r)) }
                val inner = Path().apply {
                    addOval(Rect(c.x - r * 0.62f, c.y - r * 1.08f, c.x + r * 1.38f, c.y + r * 0.92f))
                }
                drawPath(Path.combine(PathOperation.Difference, outer, inner), color = tones.body)
            }
        }

        // Bulutlar (katmanlara göre farklı hızda yatay süzülme)
        layout.clouds.forEachIndexed { i, p ->
            val speed = when (i % 3) {
                0 -> 1f
                1 -> 0.62f
                else -> 0.44f
            }
            val x = if (animatorsEnabled) ((p.x + cloudPhase * speed * 0.25f) % 1f) else p.x
            drawCloud(tones, at(x, p.y), unit * (0.16f + 0.02f * i))
        }

        // Damlalar / kar taneleri (dikey düşüş; kar sağa-sola salınır)
        layout.streaks.forEachIndexed { i, p ->
            val col = i % 6
            val y = if (animatorsEnabled) ((p.y + fallPhase * (0.7f + 0.06f * col)) % 1f) else p.y
            val sway = if (animatorsEnabled && family == DecorFamily.SNOW) {
                0.01f * sin((fallPhase * 2f + i) * Math.PI.toFloat())
            } else {
                0f
            }
            val c = at(p.x + sway, y)
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

        // Şimşek parlaması (tam ekran alfa katmanı)
        if (flashAlpha.value > 0f) {
            drawRect(color = tones.flash.copy(alpha = tones.flash.alpha * flashAlpha.value))
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

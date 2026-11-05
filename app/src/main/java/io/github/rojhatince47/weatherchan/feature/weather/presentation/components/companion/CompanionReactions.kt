package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import io.github.rojhatince47.weatherchan.core.ui.theme.CompanionMotion
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose
import kotlinx.coroutines.delay

/**
 * Pozun üst köşesinde kısa süreli Canvas sembolü. Semboller metin/emoji
 * değildir; `clearAndSetSemantics` sayesinde ekran okuyucuya sızmaz.
 */
@Composable
fun CompanionReactions(pose: CompanionPose, color: Color, animationsEnabled: Boolean, modifier: Modifier = Modifier) {
    var visible by remember(pose) { mutableStateOf(false) }
    LaunchedEffect(pose, animationsEnabled) {
        visible = animationsEnabled
        if (animationsEnabled) {
            delay(CompanionMotion.SYMBOL_MS)
            visible = false
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(CompanionMotion.SYMBOL_MS.toInt()),
        label = "companionSymbol"
    )

    if (!visible) return

    Canvas(modifier = modifier.fillMaxSize().clearAndSetSemantics { }) {
        val alpha = progress.coerceIn(0f, 1f)
        val rise = (1f - progress) * size.height * 0.25f
        val center = Offset(size.width * 0.5f, size.height * 0.5f - rise)
        val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)

        when (pose) {
            CompanionPose.CHEERFUL, CompanionPose.TAP_GIGGLE, CompanionPose.TAP_LAUGH -> {
                val r = size.minDimension * 0.28f
                drawLine(
                    color.copy(alpha = alpha),
                    Offset(center.x - r, center.y),
                    Offset(center.x + r, center.y),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color.copy(alpha = alpha),
                    Offset(center.x, center.y - r),
                    Offset(center.x, center.y + r),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }
            CompanionPose.RAINY -> {
                val path = Path().apply {
                    moveTo(center.x, center.y - size.minDimension * 0.28f)
                    quadraticTo(center.x + size.minDimension * 0.22f, center.y, center.x, center.y + size.minDimension * 0.24f)
                    quadraticTo(center.x - size.minDimension * 0.22f, center.y, center.x, center.y - size.minDimension * 0.28f)
                    close()
                }
                drawPath(path, color.copy(alpha = alpha))
            }
            CompanionPose.COLD -> {
                val r = size.minDimension * 0.26f
                repeat(3) { index ->
                    val angle = Math.toRadians((index * 60).toDouble())
                    val dx = (Math.cos(angle) * r).toFloat()
                    val dy = (Math.sin(angle) * r).toFloat()
                    drawLine(
                        color.copy(alpha = alpha),
                        Offset(center.x - dx, center.y - dy),
                        Offset(center.x + dx, center.y + dy),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round
                    )
                }
            }
            CompanionPose.WINDY -> {
                val r = size.minDimension * 0.27f
                drawArc(
                    color.copy(alpha = alpha),
                    20f,
                    250f,
                    false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(
                        r * 2f,
                        r * 2f
                    ),
                    style = stroke
                )
                drawLine(
                    color.copy(alpha = alpha),
                    Offset(center.x + r * 0.55f, center.y + r * 0.35f),
                    Offset(
                        center.x + r * 1.15f,
                        center.y + r * 0.35f
                    ),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }
            CompanionPose.CALM, CompanionPose.TAP_HUM, CompanionPose.TAP_SIGH -> {
                repeat(2) { index ->
                    val offset = index * size.minDimension * 0.16f
                    drawArc(
                        color.copy(alpha = alpha),
                        200f,
                        140f,
                        false,
                        topLeft = Offset(
                            center.x - size.minDimension * 0.18f,
                            center.y - size.minDimension * 0.20f + offset
                        ),
                        size = Size(size.minDimension * 0.36f, size.minDimension * 0.24f),
                        style = stroke
                    )
                }
            }
            CompanionPose.SLEEPY -> {
                val w = size.minDimension * 0.22f
                val h = size.minDimension * 0.16f
                repeat(2) { index ->
                    val x = center.x - w * 0.5f + index * w * 0.35f
                    val y = center.y - h * 0.5f + index * h * 0.45f
                    drawLine(color.copy(alpha = alpha), Offset(x, y), Offset(x + w, y), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(
                        color.copy(alpha = alpha),
                        Offset(x + w, y),
                        Offset(x, y + h),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color.copy(alpha = alpha),
                        Offset(x, y + h),
                        Offset(x + w, y + h),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round
                    )
                }
            }
            CompanionPose.SURPRISED -> {
                drawLine(
                    color.copy(alpha = alpha),
                    Offset(center.x, center.y - size.minDimension * 0.28f),
                    Offset(
                        center.x,
                        center.y + size.minDimension * 0.08f
                    ),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color.copy(alpha = alpha),
                    radius = size.minDimension * 0.05f,
                    center = Offset(
                        center.x,
                        center.y + size.minDimension * 0.24f
                    )
                )
            }
        }
    }
}

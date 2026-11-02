package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionColors
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose

/**
 * Görsel varlıklar gelene kadar kartı çalıştıran Canvas chibi.
 *
 * Renkler yalnız [ConditionColors] üzerinden gelir; poz başına küçük bir
 * aksesuar eklenir. Gerçek çizimler [CompanionArtAssets.overrides] ile
 * devreye girince bu composable yalnız boş pozlarda kalır.
 */
@Composable
fun CompanionPlaceholderArt(pose: CompanionPose, colors: ConditionColors, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val faceY = h * 0.40f
        val faceRadius = w * 0.27f
        val ink = colors.onContainer

        // Gövde / hırka
        drawRoundRect(
            color = colors.accent.copy(alpha = 0.35f),
            topLeft = Offset(cx - w * 0.24f, faceY + faceRadius * 0.55f),
            size = Size(w * 0.48f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )

        // Saç arkası ve yüz
        drawCircle(color = ink.copy(alpha = 0.85f), radius = faceRadius * 1.13f, center = Offset(cx, faceY))
        drawCircle(color = colors.container, radius = faceRadius, center = Offset(cx, faceY))

        // Kaküller
        drawArc(
            color = ink,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = true,
            topLeft = Offset(cx - faceRadius, faceY - faceRadius * 1.08f),
            size = Size(faceRadius * 2f, faceRadius * 1.25f)
        )

        // Gözler
        val eyeY = faceY + faceRadius * 0.12f
        val eyeDx = faceRadius * 0.42f
        val eyeRadius = faceRadius * 0.075f
        drawCircle(color = ink, radius = eyeRadius, center = Offset(cx - eyeDx, eyeY))
        drawCircle(color = ink, radius = eyeRadius, center = Offset(cx + eyeDx, eyeY))

        // Gülümseme
        drawArc(
            color = ink,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - faceRadius * 0.30f, faceY + faceRadius * 0.20f),
            size = Size(faceRadius * 0.60f, faceRadius * 0.42f),
            style = Stroke(width = faceRadius * 0.07f, cap = StrokeCap.Round)
        )

        // Yanaklar
        drawCircle(
            color = colors.accent.copy(alpha = 0.55f),
            radius = faceRadius * 0.13f,
            center = Offset(cx - faceRadius * 0.58f, faceY + faceRadius * 0.38f)
        )
        drawCircle(
            color = colors.accent.copy(alpha = 0.55f),
            radius = faceRadius * 0.13f,
            center = Offset(cx + faceRadius * 0.58f, faceY + faceRadius * 0.38f)
        )

        when (pose) {
            CompanionPose.CHEERFUL -> {
                val sparkle = faceRadius * 0.12f
                listOf(
                    Offset(w * 0.18f, h * 0.18f),
                    Offset(w * 0.82f, h * 0.22f),
                    Offset(w * 0.14f, h * 0.55f)
                ).forEach { center ->
                    drawLine(
                        colors.accent,
                        center.copy(y = center.y - sparkle),
                        center.copy(y = center.y + sparkle),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        colors.accent,
                        center.copy(x = center.x - sparkle),
                        center.copy(x = center.x + sparkle),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }
            CompanionPose.RAINY -> {
                drawArc(
                    color = colors.accent,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - w * 0.30f, h * 0.10f),
                    size = Size(w * 0.60f, h * 0.30f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
                drawLine(colors.accent, Offset(cx, h * 0.25f), Offset(cx, h * 0.40f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawCircle(colors.accent.copy(alpha = 0.7f), radius = 3f, center = Offset(w * 0.20f, h * 0.30f))
                drawCircle(colors.accent.copy(alpha = 0.7f), radius = 3f, center = Offset(w * 0.80f, h * 0.34f))
            }
            CompanionPose.COLD -> {
                drawLine(
                    color = colors.accent,
                    start = Offset(cx - faceRadius * 0.75f, faceY + faceRadius * 1.05f),
                    end = Offset(cx + faceRadius * 0.75f, faceY + faceRadius * 1.05f),
                    strokeWidth = faceRadius * 0.20f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    colors.accent.copy(alpha = 0.35f),
                    radius = 5f,
                    center = Offset(
                        cx + faceRadius * 1.15f,
                        faceY + faceRadius * 0.55f
                    )
                )
                drawCircle(
                    colors.accent.copy(alpha = 0.25f),
                    radius = 7f,
                    center = Offset(
                        cx + faceRadius * 1.30f,
                        faceY + faceRadius * 0.30f
                    )
                )
            }
            CompanionPose.WINDY -> {
                repeat(3) { index ->
                    val y = faceY - faceRadius * 0.55f + index * faceRadius * 0.35f
                    drawLine(
                        color = ink.copy(alpha = 0.75f),
                        start = Offset(cx + faceRadius * 0.75f, y),
                        end = Offset(w * 0.95f, y - faceRadius * 0.20f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }
            CompanionPose.CALM -> {
                drawRoundRect(
                    color = colors.accent.copy(alpha = 0.45f),
                    topLeft = Offset(cx - w * 0.13f, faceY + faceRadius * 0.90f),
                    size = Size(w * 0.26f, h * 0.10f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
                drawArc(
                    color = colors.accent.copy(alpha = 0.55f),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(cx - w * 0.10f, faceY + faceRadius * 0.55f),
                    size = Size(w * 0.20f, h * 0.10f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
            CompanionPose.SLEEPY -> {
                repeat(3) { index ->
                    val x = w * 0.70f + index * w * 0.07f
                    val y = h * 0.20f - index * h * 0.06f
                    drawLine(colors.accent, Offset(x, y), Offset(x + w * 0.08f, y), strokeWidth = 3f, cap = StrokeCap.Round)
                    drawLine(colors.accent, Offset(x + w * 0.08f, y), Offset(x, y + h * 0.05f), strokeWidth = 3f, cap = StrokeCap.Round)
                    drawLine(
                        colors.accent,
                        Offset(x, y + h * 0.05f),
                        Offset(x + w * 0.08f, y + h * 0.05f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }
            CompanionPose.SURPRISED -> {
                drawLine(colors.accent, Offset(cx, h * 0.08f), Offset(cx, h * 0.20f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawCircle(colors.accent, radius = 3f, center = Offset(cx, h * 0.25f))
                drawCircle(ink, radius = faceRadius * 0.12f, center = Offset(cx, faceY + faceRadius * 0.55f))
            }
        }
    }
}

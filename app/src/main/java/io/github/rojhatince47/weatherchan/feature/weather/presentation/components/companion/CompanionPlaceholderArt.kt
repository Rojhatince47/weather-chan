package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionColors
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose

/**
 * Görsel varlıklar gelene kadar kartı çalıştıran Canvas chibi.
 *
 * Tam boy, beş kafa boyu bir yer tutucudur; gerçek çizimler
 * [CompanionArtAssets.overrides] ile devreye girince yalnız boş pozlarda
 * kalır. Renkler yalnız [ConditionColors] üzerinden gelir.
 */
@Composable
fun CompanionPlaceholderArt(pose: CompanionPose, colors: ConditionColors, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val headR = h * 0.10f
        val headY = h * 0.155f
        val neckY = headY + headR * 0.95f
        val shoulderY = neckY + h * 0.01f
        val hipY = h * 0.56f
        val ankleY = h * 0.885f
        val bodyHalf = w * 0.17f
        val darkContainer = colors.container.luminance() < 0.5f
        val face = if (darkContainer) colors.onContainer else colors.container
        val hair = if (darkContainer) colors.container else colors.onContainer
        val feature = if (darkContainer) colors.container else colors.onContainer
        val line = colors.onContainer
        val accent = colors.accent

        // Arka saç ve örgüler
        drawCircle(hair.copy(alpha = 0.85f), radius = headR * 1.18f, center = Offset(cx, headY))
        listOf(-1f, 1f).forEach { side ->
            drawRoundRect(
                color = hair.copy(alpha = 0.78f),
                topLeft = Offset(cx + side * headR * 1.02f - headR * 0.16f, headY + headR * 0.35f),
                size = Size(headR * 0.32f, headR * 1.45f),
                cornerRadius = CornerRadius(headR * 0.16f, headR * 0.16f)
            )
        }

        // Bacaklar ve botlar
        val legW = w * 0.072f
        listOf(-1f, 1f).forEach { side ->
            val legX = cx + side * w * 0.095f - legW * 0.5f
            drawRoundRect(
                color = line.copy(alpha = 0.22f),
                topLeft = Offset(legX, hipY + h * 0.04f),
                size = Size(legW, ankleY - hipY),
                cornerRadius = CornerRadius(legW * 0.4f, legW * 0.4f)
            )
            drawRoundRect(
                color = line.copy(alpha = 0.82f),
                topLeft = Offset(legX - legW * 0.18f, ankleY - h * 0.01f),
                size = Size(legW * 1.36f, h * 0.055f),
                cornerRadius = CornerRadius(legW * 0.5f, legW * 0.5f)
            )
        }

        // Etek
        val skirtTop = hipY - h * 0.015f
        val skirtBottom = hipY + h * 0.11f
        val skirt = Path().apply {
            moveTo(cx - w * 0.16f, skirtTop)
            lineTo(cx + w * 0.16f, skirtTop)
            lineTo(cx + w * 0.23f, skirtBottom)
            lineTo(cx - w * 0.23f, skirtBottom)
            close()
        }
        drawPath(skirt, accent.copy(alpha = 0.55f))

        // Gövde / hırka
        drawRoundRect(
            color = accent.copy(alpha = 0.36f),
            topLeft = Offset(cx - bodyHalf, shoulderY),
            size = Size(bodyHalf * 2f, hipY - shoulderY),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )

        // Kollar: poza göre
        fun arm(fromX: Float, fromY: Float, toX: Float, toY: Float) {
            drawLine(
                color = accent.copy(alpha = 0.55f),
                start = Offset(fromX, fromY),
                end = Offset(toX, toY),
                strokeWidth = w * 0.055f,
                cap = StrokeCap.Round
            )
        }

        when (pose) {
            CompanionPose.CHEERFUL, CompanionPose.TAP_GIGGLE, CompanionPose.TAP_LAUGH -> {
                arm(cx - bodyHalf * 0.9f, shoulderY + h * 0.04f, cx - bodyHalf - w * 0.03f, hipY - h * 0.08f)
                arm(cx + bodyHalf * 0.9f, shoulderY + h * 0.04f, cx + bodyHalf + w * 0.08f, shoulderY - h * 0.04f)
            }
            CompanionPose.RAINY -> {
                arm(cx - bodyHalf * 0.9f, shoulderY + h * 0.04f, cx - w * 0.20f, hipY - h * 0.10f)
                arm(cx + bodyHalf * 0.9f, shoulderY + h * 0.04f, cx + headR * 1.35f, shoulderY + h * 0.08f)
            }
            CompanionPose.COLD -> {
                arm(cx - bodyHalf * 0.9f, shoulderY + h * 0.04f, cx - w * 0.055f, shoulderY + h * 0.18f)
                arm(cx + bodyHalf * 0.9f, shoulderY + h * 0.04f, cx + w * 0.055f, shoulderY + h * 0.18f)
            }
            CompanionPose.WINDY -> {
                arm(cx - bodyHalf * 0.9f, shoulderY + h * 0.04f, cx - w * 0.10f, shoulderY - h * 0.08f)
                arm(cx + bodyHalf * 0.9f, shoulderY + h * 0.04f, cx + w * 0.12f, hipY - h * 0.10f)
            }
            CompanionPose.CALM, CompanionPose.TAP_HUM, CompanionPose.TAP_SIGH -> {
                arm(cx - bodyHalf * 0.9f, shoulderY + h * 0.04f, cx - w * 0.075f, shoulderY + h * 0.16f)
                arm(cx + bodyHalf * 0.9f, shoulderY + h * 0.04f, cx + w * 0.075f, shoulderY + h * 0.16f)
            }
            CompanionPose.SLEEPY -> {
                arm(cx - bodyHalf * 0.9f, shoulderY + h * 0.04f, cx - w * 0.055f, hipY - h * 0.10f)
                arm(cx + bodyHalf * 0.9f, shoulderY + h * 0.04f, cx + w * 0.045f, headY + h * 0.08f)
            }
            CompanionPose.SURPRISED -> {
                arm(cx - bodyHalf * 0.9f, shoulderY + h * 0.04f, cx - w * 0.09f, headY + h * 0.05f)
                arm(cx + bodyHalf * 0.9f, shoulderY + h * 0.04f, cx + w * 0.09f, headY + h * 0.05f)
            }
        }

        // Yüz
        drawCircle(face, radius = headR, center = Offset(cx, headY))
        drawArc(
            color = hair,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(cx - headR, headY - headR * 1.08f),
            size = Size(headR * 2f, headR * 1.25f),
            style = Stroke(width = headR * 0.12f, cap = StrokeCap.Round)
        )

        val eyeY = headY + headR * 0.15f
        val eyeDx = headR * 0.38f
        val eyeRadius = headR * 0.075f
        if (pose == CompanionPose.SLEEPY) {
            listOf(-1f, 1f).forEach { side ->
                drawLine(
                    color = feature,
                    start = Offset(cx + side * eyeDx - eyeRadius, eyeY),
                    end = Offset(cx + side * eyeDx + eyeRadius, eyeY),
                    strokeWidth = headR * 0.05f,
                    cap = StrokeCap.Round
                )
            }
        } else {
            drawCircle(feature, radius = eyeRadius, center = Offset(cx - eyeDx, eyeY))
            drawCircle(feature, radius = eyeRadius, center = Offset(cx + eyeDx, eyeY))
        }

        if (pose == CompanionPose.SURPRISED) {
            drawCircle(feature, radius = headR * 0.10f, center = Offset(cx, headY + headR * 0.52f))
        } else {
            drawArc(
                color = feature,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - headR * 0.30f, headY + headR * 0.28f),
                size = Size(headR * 0.60f, headR * 0.42f),
                style = Stroke(width = headR * 0.07f, cap = StrokeCap.Round)
            )
        }

        // Yanaklar
        listOf(-1f, 1f).forEach { side ->
            drawCircle(
                color = accent.copy(alpha = 0.55f),
                radius = headR * 0.13f,
                center = Offset(cx + side * headR * 0.58f, headY + headR * 0.40f)
            )
        }

        // Poz aksesuarları
        when (pose) {
            CompanionPose.CHEERFUL, CompanionPose.TAP_GIGGLE, CompanionPose.TAP_LAUGH -> {
                val sparkle = headR * 0.12f
                listOf(
                    Offset(w * 0.18f, h * 0.12f),
                    Offset(w * 0.82f, h * 0.18f),
                    Offset(w * 0.14f, h * 0.42f)
                ).forEach { center ->
                    drawLine(
                        accent,
                        center.copy(y = center.y - sparkle),
                        center.copy(y = center.y + sparkle),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        accent,
                        center.copy(x = center.x - sparkle),
                        center.copy(x = center.x + sparkle),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }
            CompanionPose.RAINY -> {
                drawArc(
                    color = accent,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - w * 0.30f, h * 0.015f),
                    size = Size(w * 0.60f, h * 0.14f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
                drawLine(
                    accent,
                    Offset(cx + headR * 1.35f, h * 0.08f),
                    Offset(cx + headR * 1.35f, shoulderY + h * 0.10f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
            CompanionPose.COLD -> {
                drawLine(
                    color = accent,
                    start = Offset(cx - headR * 0.85f, neckY),
                    end = Offset(cx + headR * 0.85f, neckY),
                    strokeWidth = h * 0.028f,
                    cap = StrokeCap.Round
                )
            }
            CompanionPose.WINDY -> {
                repeat(3) { index ->
                    val y = headY - headR * 0.55f + index * headR * 0.35f
                    drawLine(
                        color = line.copy(alpha = 0.75f),
                        start = Offset(cx + headR * 0.75f, y),
                        end = Offset(w * 0.95f, y - headR * 0.20f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }
            CompanionPose.CALM, CompanionPose.TAP_HUM, CompanionPose.TAP_SIGH -> {
                val cupTop = shoulderY + h * 0.13f
                drawRoundRect(
                    color = accent.copy(alpha = 0.55f),
                    topLeft = Offset(cx - w * 0.075f, cupTop),
                    size = Size(w * 0.15f, h * 0.055f),
                    cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
                )
                drawArc(
                    color = accent.copy(alpha = 0.55f),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(cx - w * 0.045f, cupTop - h * 0.045f),
                    size = Size(w * 0.09f, h * 0.05f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
            CompanionPose.SLEEPY -> {
                drawRoundRect(
                    color = accent.copy(alpha = 0.24f),
                    topLeft = Offset(cx - bodyHalf * 1.25f, shoulderY - h * 0.02f),
                    size = Size(bodyHalf * 2.5f, hipY - shoulderY + h * 0.02f),
                    cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
                )
                repeat(3) { index ->
                    val x = w * 0.70f + index * w * 0.07f
                    val y = h * 0.18f - index * h * 0.055f
                    drawLine(accent, Offset(x, y), Offset(x + w * 0.08f, y), strokeWidth = 3f, cap = StrokeCap.Round)
                    drawLine(accent, Offset(x + w * 0.08f, y), Offset(x, y + h * 0.045f), strokeWidth = 3f, cap = StrokeCap.Round)
                    drawLine(
                        accent,
                        Offset(x, y + h * 0.045f),
                        Offset(x + w * 0.08f, y + h * 0.045f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }
            CompanionPose.SURPRISED -> {
                drawLine(
                    accent,
                    Offset(cx, h * 0.025f),
                    Offset(cx, h * 0.075f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                drawCircle(accent, radius = 3f, center = Offset(cx, h * 0.10f))
            }
        }
    }
}

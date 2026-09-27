package io.github.rojhatince47.weatherchan.core.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tema simgesi. material-icons-extended eklemeden Canvas ile çizilir:
 * LIGHT = güneş, DARK = hilal, SYSTEM = yarı güneş.
 */
@Composable
fun ThemeModeIcon(
    themeMode: ThemeMode,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    cutoutColor: Color = MaterialTheme.colorScheme.surface
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.26f
        val rayLength = size.minDimension * 0.16f
        val stroke = size.minDimension * 0.09f

        when (themeMode) {
            ThemeMode.LIGHT -> {
                drawCircle(color = tint, radius = radius, center = center)
                repeat(8) { index ->
                    val angle = index * (2 * Math.PI / 8)
                    val start = Offset(
                        x = center.x + cos(angle).toFloat() * (radius + rayLength * 0.4f),
                        y = center.y + sin(angle).toFloat() * (radius + rayLength * 0.4f)
                    )
                    val end = Offset(
                        x = center.x + cos(angle).toFloat() * (radius + rayLength),
                        y = center.y + sin(angle).toFloat() * (radius + rayLength)
                    )
                    drawLine(
                        color = tint,
                        start = start,
                        end = end,
                        strokeWidth = stroke
                    )
                }
            }

            ThemeMode.DARK -> {
                // Ana disk; ardından içinden kaydırılan ikinci disk hilal yarımını keser.
                drawCircle(color = tint, radius = radius, center = center)
                drawCircle(
                    color = cutoutColor,
                    radius = radius * 0.92f,
                    center = Offset(center.x + radius * 0.45f, center.y - radius * 0.30f)
                )
            }

            ThemeMode.SYSTEM -> {
                drawCircle(
                    color = tint,
                    radius = radius,
                    center = center,
                    style = Stroke(width = stroke * 0.6f)
                )
                drawLine(
                    color = tint,
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x, center.y + radius),
                    strokeWidth = stroke * 0.6f
                )
            }
        }
    }
}

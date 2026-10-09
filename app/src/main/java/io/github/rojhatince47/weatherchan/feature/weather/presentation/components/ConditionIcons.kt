package io.github.rojhatince47.weatherchan.feature.weather.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherIcon

private val CrescentColor = Color(0xFFC5CBD6)
private val CloudFill = Color(0xFFEEF1F5)
private val CloudEdge = Color(0xFF9AA3B2)

/**
 * Koşul simgesi — tek yuva. Glifler metin, [WeatherIcon.MoonBehindCloud]
 * iki tonlu Canvas çizimidir (gümüş hilal sağ üst, beyaz bulut sol alt,
 * hilalin ~%35'ini örter).
 */
@Composable
fun ConditionIcon(icon: WeatherIcon, size: Dp, modifier: Modifier = Modifier) {
    when (icon) {
        is WeatherIcon.Glyph -> Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon.value,
                style = LocalTextStyle.current.copy(fontSize = size.value.sp * 0.8f),
                maxLines = 1
            )
        }

        is WeatherIcon.MoonBehindCloud -> Canvas(modifier = modifier.size(size).clipToBounds()) {
            val s = this.size.minDimension
            val moonR = 0.21f * s
            val outer = Path().apply {
                addOval(
                    Rect(
                        offset = Offset(0.66f * s - moonR, 0.34f * s - moonR),
                        size = Size(moonR * 2, moonR * 2)
                    )
                )
            }
            val inner = Path().apply {
                addOval(
                    Rect(
                        offset = Offset(0.56f * s - moonR, 0.30f * s - moonR),
                        size = Size(moonR * 2, moonR * 2)
                    )
                )
            }
            drawPath(Path.combine(PathOperation.Difference, outer, inner), color = CrescentColor)

            val cloudCircles = listOf(
                Triple(0.30f * s, 0.55f * s, 0.14f * s),
                Triple(0.50f * s, 0.50f * s, 0.18f * s),
                Triple(0.68f * s, 0.58f * s, 0.12f * s)
            )
            val bodyTopLeft = Offset(0.16f * s, 0.55f * s)
            val bodySize = Size(0.64f * s, 0.22f * s)
            val corner = CornerRadius(0.08f * s, 0.08f * s)
            val edge = 1.5.dp.toPx()

            cloudCircles.forEach { (cx, cy, r) ->
                drawCircle(color = CloudEdge, radius = r + edge, center = Offset(cx, cy))
            }
            drawRoundRect(
                color = CloudEdge,
                topLeft = Offset(bodyTopLeft.x - edge, bodyTopLeft.y),
                size = Size(bodySize.width + 2 * edge, bodySize.height + edge),
                cornerRadius = corner
            )
            cloudCircles.forEach { (cx, cy, r) ->
                drawCircle(color = CloudFill, radius = r, center = Offset(cx, cy))
            }
            drawRoundRect(
                color = CloudFill,
                topLeft = bodyTopLeft,
                size = bodySize,
                cornerRadius = corner
            )
        }
    }
}

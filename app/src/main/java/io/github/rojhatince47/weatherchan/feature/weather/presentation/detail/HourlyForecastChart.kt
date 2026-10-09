package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.HourlyForecastUiModel

private val ChartBandHeight = 96.dp
private val ChartYPad = 16.dp
private val CellWidth = 64.dp
private val CellBandHeight = 72.dp

/**
 * Saatlik tahmini sıcaklık çizgisi + noktalarla, altında sabit genişlikli
 * hücrelerle sunar. Yatay kaydırılır; hücrenin yatay merkezi o saatin
 * nokta x'i ile birebir hizalıdır. Hücre metrikleri kasıtlı sabittir (hizalama
 * grafiğe bağlıdır); büyük fontScale'de metin sarılabilir/kırpılabilir.
 */
@Composable
fun HourlyForecastChart(items: List<HourlyForecastUiModel>, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val cellWidthPx = with(density) { CellWidth.toPx() }
    val chartHeightPx = with(density) { ChartBandHeight.toPx() }
    val padPx = with(density) { ChartYPad.toPx() }
    val totalWidth = with(density) { (cellWidthPx * items.size).toDp() }
    val points = remember(items, cellWidthPx, chartHeightPx, padPx) {
        chartPoints(items.map { it.temperatureValue }, cellWidthPx, chartHeightPx, padPx)
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hourly_chart"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Box(
            Modifier
                .horizontalScroll(rememberScrollState())
                .width(totalWidth)
        ) {
            Column {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(ChartBandHeight)
                ) {
                    points.zipWithNext { a, b ->
                        drawLine(
                            color = lineColor,
                            start = Offset(a.first, a.second),
                            end = Offset(b.first, b.second),
                            strokeWidth = 2.5.dp.toPx()
                        )
                    }
                    points.forEach { (x, y) ->
                        drawCircle(color = dotColor, radius = 4.dp.toPx(), center = Offset(x, y))
                    }
                }
                Row {
                    items.forEach { item -> HourlyChartCell(item) }
                }
            }
        }
    }
}

@Composable
private fun HourlyChartCell(item: HourlyForecastUiModel) {
    Column(
        modifier = Modifier
            .width(CellWidth)
            .height(CellBandHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = item.timeText, style = MaterialTheme.typography.labelSmall)
        Text(text = item.conditionEmoji)
        Text(text = item.temperatureText)
        Text(
            text = item.precipitationProbabilityText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Nokta (x, y) piksel hesapları — Compose'dan bağımsız saf fonksiyon.
 * x_i = i * cellWidthPx + cellWidthPx/2; y min–max ölçekli (eşitse ortalı).
 */
internal fun chartPoints(values: List<Int>, cellWidthPx: Float, chartHeightPx: Float, padPx: Float): List<Pair<Float, Float>> {
    if (values.isEmpty()) return emptyList()
    val min = values.minOrNull() ?: return emptyList()
    val max = values.maxOrNull() ?: return emptyList()
    val usable = (chartHeightPx - 2 * padPx).coerceAtLeast(0f)
    return values.mapIndexed { index, value ->
        val x = index * cellWidthPx + cellWidthPx / 2f
        val fraction = if (max == min) 0.5f else (value - min).toFloat() / (max - min)
        val y = chartHeightPx - padPx - fraction * usable
        x to y
    }
}

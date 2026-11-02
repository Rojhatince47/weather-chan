package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionColors
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CompanionUiModel

/**
 * Ana sayfa şehir sayfasındaki Weather-chan kartı.
 *
 * Statik sürümde dokunuş henüz bağlı değildir; [animationsEnabled] ve [audio]
 * ileriki adımlarda davranış kazanır. Varsayılanlar testler ve preview'lar
 * için sessiz/statiktir.
 */
@Composable
fun CompanionCard(
    advice: CompanionUiModel,
    colors: ConditionColors,
    audio: CompanionAudioPlayer = NoOpCompanionAudioPlayer,
    animationsEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val artDescription = stringResource(
        R.string.companion_art_description,
        stringResource(advice.pose.labelRes())
    )
    val bubbleShape = RoundedCornerShape(20.dp)
    val bubbleColor = lerp(colors.container, colors.accent, 0.08f)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("companion_card")
            .semantics(mergeDescendants = true) { }
            .clickable(onClickLabel = stringResource(R.string.companion_tap_action)) {
                // Dokunuş davranışı Task 6'da bağlanır.
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = colors.container,
            contentColor = colors.onContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 112.dp, height = 140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.accent.copy(alpha = 0.28f), Color.Transparent)
                        )
                    )
                    .testTag("companion_art")
                    .semantics { contentDescription = artDescription }
            ) {
                val override = CompanionArtAssets.overrides[advice.pose]
                if (override != null) {
                    Image(
                        painter = painterResource(override),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CompanionPlaceholderArt(
                        pose = advice.pose,
                        colors = colors,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(bubbleColor, bubbleShape)
                    .border(1.dp, colors.accent.copy(alpha = 0.35f), bubbleShape)
                    .drawBehind {
                        val tailWidth = 10.dp.toPx()
                        val tailHeight = 14.dp.toPx()
                        val x = if (isRtl) size.width else 0f
                        val direction = if (isRtl) 1f else -1f
                        val top = size.height * 0.35f
                        val path = Path().apply {
                            moveTo(x, top)
                            lineTo(x + direction * tailWidth, top + tailHeight / 2f)
                            lineTo(x, top + tailHeight)
                            close()
                        }
                        drawPath(path, bubbleColor)
                    }
                    .padding(12.dp)
            ) {
                Text(
                    text = adviceLine(advice.category, advice.variantSeed),
                    modifier = Modifier.testTag("companion_bubble"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onContainer
                )
            }
        }
    }
}

package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
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
import io.github.rojhatince47.weatherchan.core.ui.theme.CompanionMotion
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionColors
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaMotion
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CompanionUiModel
import kotlinx.coroutines.delay

/**
 * Ana sayfa şehir sayfasındaki Weather-chan kartı.
 *
 * Dokunuş kısa bir [CompanionPose.SURPRISED] tepkisi başlatır; tepki
 * `CompanionMotion.REACTION_MS` sonra hava repliğine döner. Animasyonlar
 * [animationsEnabled] ile kapatılabilir; kapalıyken poz ve metin anlıktır.
 */
@Composable
fun CompanionCard(
    advice: CompanionUiModel,
    colors: ConditionColors,
    audio: CompanionAudioPlayer = NoOpCompanionAudioPlayer,
    animationsEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    var reacting by remember(advice.pose, advice.category, advice.variantSeed) { mutableStateOf(false) }
    var tapCount by remember(advice.pose, advice.category, advice.variantSeed) { mutableIntStateOf(0) }
    var lastTapAt by remember(advice.pose, advice.category, advice.variantSeed) {
        mutableLongStateOf(-CompanionMotion.TAP_DEBOUNCE_MS)
    }
    val effectivePose = if (reacting) CompanionPose.SURPRISED else advice.pose
    val artDescription = stringResource(
        R.string.companion_art_description,
        stringResource(effectivePose.labelRes())
    )
    val bubbleShape = RoundedCornerShape(20.dp)
    val bubbleColor = lerp(colors.container, colors.accent, 0.08f)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val breathingScale = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "companion_breath")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.015f,
            animationSpec = infiniteRepeatable(
                animation = tween(CompanionMotion.BREATH_MS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breath"
        )
        scale
    } else {
        1f
    }

    val bounce = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(tapCount) {
        if (animationsEnabled && tapCount > 0) {
            bounce.snapTo(0.94f)
            bounce.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            rotation.snapTo(-2.5f)
            rotation.animateTo(
                targetValue = 2.5f,
                animationSpec = tween(HavaMotion.FAST)
            )
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(HavaMotion.FAST)
            )
        }
    }
    LaunchedEffect(reacting, tapCount) {
        if (reacting) {
            delay(CompanionMotion.REACTION_MS)
            reacting = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("companion_card")
            .semantics(mergeDescendants = true) { }
            .clickable(onClickLabel = stringResource(R.string.companion_tap_action)) {
                val now = SystemClock.uptimeMillis()
                if (now - lastTapAt >= CompanionMotion.TAP_DEBOUNCE_MS) {
                    lastTapAt = now
                    reacting = true
                    tapCount += 1
                }
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
                AnimatedContent(
                    targetState = effectivePose,
                    transitionSpec = {
                        if (animationsEnabled) {
                            fadeIn(tween(HavaMotion.MEDIUM)) + scaleIn(initialScale = 0.96f) togetherWith
                                fadeOut(tween(HavaMotion.MEDIUM))
                        } else {
                            EnterTransition.None togetherWith ExitTransition.None
                        }
                    },
                    label = "companionPose"
                ) { pose ->
                    val artModifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = breathingScale * bounce.value,
                            scaleY = breathingScale * bounce.value,
                            rotationZ = rotation.value
                        )
                    val override = CompanionArtAssets.overrides[pose]
                    if (override != null) {
                        Image(
                            painter = painterResource(override),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = artModifier
                        )
                    } else {
                        CompanionPlaceholderArt(
                            pose = pose,
                            colors = colors,
                            modifier = artModifier
                        )
                    }
                }
                CompanionReactions(
                    pose = effectivePose,
                    color = colors.accent,
                    animationsEnabled = animationsEnabled,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                )
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
                AnimatedContent(
                    targetState = reacting,
                    transitionSpec = {
                        if (animationsEnabled) {
                            fadeIn(tween(HavaMotion.FAST)) togetherWith fadeOut(tween(HavaMotion.FAST))
                        } else {
                            EnterTransition.None togetherWith ExitTransition.None
                        }
                    },
                    label = "companionText"
                ) { isReacting ->
                    Text(
                        text = if (isReacting) {
                            tapLine(advice.variantSeed, tapCount)
                        } else {
                            adviceLine(advice.category, advice.variantSeed)
                        },
                        modifier = Modifier.testTag("companion_bubble"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onContainer
                    )
                }
            }
        }
    }
}

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.theme.CompanionMotion
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionColors
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaMotion
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose
import io.github.rojhatince47.weatherchan.feature.weather.presentation.components.ConditionIcon
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CompanionUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CurrentWeatherUiModel
import kotlinx.coroutines.delay

/**
 * Ana sayfanın hero kartı: Weather-chan, anlık hava ve tavsiye tek yüzeyde.
 *
 * Hava metinleri etkileşimsizdir; yalnız karakter panosu ve tavsiye balonu
 * dokunuşu kabul eder. Hero merge edilmez; a11y'de hava, karakter ve tavsiye
 * ayrı odak noktalarıdır.
 */
@Composable
fun CompanionHero(
    advice: CompanionUiModel,
    currentWeather: CurrentWeatherUiModel,
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
    val tapActionLabel = stringResource(R.string.companion_tap_action)
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
            rotation.animateTo(targetValue = 2.5f, animationSpec = tween(HavaMotion.FAST))
            rotation.animateTo(targetValue = 0f, animationSpec = tween(HavaMotion.FAST))
        }
    }
    LaunchedEffect(reacting, tapCount) {
        if (reacting) {
            delay(CompanionMotion.REACTION_MS)
            reacting = false
        }
    }

    val onTap: () -> Unit = {
        val now = SystemClock.uptimeMillis()
        if (now - lastTapAt >= CompanionMotion.TAP_DEBOUNCE_MS) {
            lastTapAt = now
            reacting = true
            tapCount += 1
            audio.playTap()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("companion_hero"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = colors.container,
            contentColor = colors.onContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 136.dp, height = 170.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(colors.accent.copy(alpha = 0.28f), Color.Transparent)
                            )
                        )
                        .testTag("companion_art")
                        .semantics { contentDescription = artDescription }
                        .clickable(onClickLabel = tapActionLabel, onClick = onTap)
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

                Spacer(Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("companion_weather"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_current),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConditionIcon(icon = currentWeather.icon, size = 36.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = currentWeather.temperatureText,
                            style = MaterialTheme.typography.displaySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = currentWeather.conditionLabel.asString(),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = currentWeather.windSpeedText,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = currentWeather.humidityText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .testTag("companion_tail")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = if (isRtl) size.width - 68.dp.toPx() else 68.dp.toPx()
                    val half = 6.dp.toPx()
                    val path = Path().apply {
                        moveTo(centerX - half, size.height)
                        lineTo(centerX + half, size.height)
                        lineTo(centerX, 0f)
                        close()
                    }
                    drawPath(path, bubbleColor)
                    drawPath(
                        path = path,
                        color = colors.accent.copy(alpha = 0.35f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("companion_bubble")
                    .semantics(mergeDescendants = true) { }
                    .background(bubbleColor, bubbleShape)
                    .border(1.dp, colors.accent.copy(alpha = 0.35f), bubbleShape)
                    .clickable(onClickLabel = tapActionLabel, onClick = onTap)
                    .padding(14.dp)
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onContainer
                    )
                }
            }
        }
    }
}

package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlinx.coroutines.delay

/** Hava durumuna göre kısa çevre sesi çalar; döngü yoktur. */
internal interface CompanionAmbientPlayer {
    fun play(clip: CompanionAmbientClip)

    fun release()
}

/** Test/preview varsayılanı: hiçbir ses çıkarmaz. */
internal object NoOpCompanionAmbientPlayer : CompanionAmbientPlayer {
    override fun play(clip: CompanionAmbientClip) = Unit

    override fun release() = Unit
}

internal object CompanionAmbientAssets {
    fun resFor(clip: CompanionAmbientClip): Int = when (clip) {
        CompanionAmbientClip.BIRDS -> R.raw.companion_ambient_birds
        CompanionAmbientClip.CRICKETS -> R.raw.companion_ambient_crickets
        CompanionAmbientClip.WIND -> R.raw.companion_ambient_wind
        CompanionAmbientClip.RAIN -> R.raw.companion_ambient_rain
        CompanionAmbientClip.SNOW -> R.raw.companion_ambient_snow
        CompanionAmbientClip.STORM -> R.raw.companion_ambient_storm
    }
}

/**
 * Tek SoundPool örneği; çevre sesleri kısa olduğu için tek akış yeterli.
 * Yükleme bitmeden gelen istek sessizce atlanır.
 */
@Composable
internal fun rememberCompanionAmbientPlayer(): CompanionAmbientPlayer {
    val context = LocalContext.current.applicationContext
    val player = remember(context) {
        runCatching {
            SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .build()
        }.getOrNull()
    } ?: return NoOpCompanionAmbientPlayer

    val loaded = remember(player) { ConcurrentHashMap.newKeySet<Int>() }
    val sampleIdByResId = remember(player) { ConcurrentHashMap<Int, Int>() }

    DisposableEffect(player) {
        player.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
        CompanionAmbientClip.entries.forEach { clip ->
            val resId = CompanionAmbientAssets.resFor(clip)
            runCatching { player.load(context, resId, 1) }
                .getOrNull()
                ?.takeIf { it != 0 }
                ?.let { sampleIdByResId[resId] = it }
        }
        onDispose { runCatching { player.release() } }
    }

    return remember(player, loaded) {
        object : CompanionAmbientPlayer {
            private var lastStreamId = 0

            override fun play(clip: CompanionAmbientClip) {
                val resId = CompanionAmbientAssets.resFor(clip)
                val sampleId = sampleIdForClip(listOf(resId), 0, sampleIdByResId, loaded) ?: return
                if (lastStreamId != 0) runCatching { player.stop(lastStreamId) }
                lastStreamId = runCatching {
                    player.play(sampleId, 0.20f, 0.20f, 1, 0, 1f)
                }.getOrDefault(0)
            }

            override fun release() = Unit
        }
    }
}

/**
 * Ekran görünürken hava koşuluna göre ara ara tek çevre sesi çalar.
 * İlk ses kısa bir gecikmeyle gelir; sonrasında 20–35 sn arayla tekrarlar.
 */
@Composable
internal fun AmbientWeatherEffect(
    cityId: Long?,
    condition: WeatherCondition?,
    isDay: Boolean,
    enabled: Boolean,
    player: CompanionAmbientPlayer
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(enabled, cityId, condition, isDay, player, lifecycleOwner) {
        if (!enabled || cityId == null || condition == null) return@LaunchedEffect
        val clip = ambientClipFor(condition, isDay)
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(1_200)
            player.play(clip)
            while (true) {
                delay(Random.nextLong(20_000L, 35_000L))
                player.play(clip)
            }
        }
    }
}

package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.ConcurrentHashMap

/** Dokunuş sesi portu; üretimde [rememberCompanionAudioPlayer] kullanılır. */
interface CompanionAudioPlayer {
    fun playTap(clipIndex: Int)

    fun release()
}

/** Test/preview varsayılanı: hiçbir ses çıkarmaz. */
object NoOpCompanionAudioPlayer : CompanionAudioPlayer {
    override fun playTap(clipIndex: Int) = Unit

    override fun release() = Unit
}

/**
 * Tek SoundPool örneği. Yükleme asenkron olduğu için ilk dokunuş sesi
 * atlanabilir; metin/animasyon etkilenmez. Tüm SoundPool çağrıları
 * `runCatching` ile sarılır; hata sessiz no-op'a düşer.
 */
@Composable
fun rememberCompanionAudioPlayer(enabled: Boolean = true): CompanionAudioPlayer {
    val context = LocalContext.current.applicationContext
    val enabledState = rememberUpdatedState(enabled)
    val audioManager = remember(context) { context.getSystemService(AudioManager::class.java) }
    val player = remember(context) {
        runCatching {
            SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
        }.getOrNull()
    } ?: return NoOpCompanionAudioPlayer

    val loaded = remember(player) { ConcurrentHashMap.newKeySet<Int>() }
    val sampleIdByResId = remember(player) { ConcurrentHashMap<Int, Int>() }

    DisposableEffect(player) {
        player.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
        CompanionSoundAssets.tapClips.forEach { id ->
            runCatching { player.load(context, id, 1) }
                .getOrNull()
                ?.takeIf { it != 0 }
                ?.let { sampleIdByResId[id] = it }
        }
        onDispose { runCatching { player.release() } }
    }

    return remember(player, loaded) {
        object : CompanionAudioPlayer {
            override fun playTap(clipIndex: Int) {
                val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
                val silentMode = ringerMode != AudioManager.RINGER_MODE_NORMAL
                if (!shouldPlayCompanionSound(enabledState.value, silentMode)) return

                val sampleId = sampleIdForClip(
                    CompanionSoundAssets.tapClips,
                    clipIndex,
                    sampleIdByResId,
                    loaded
                ) ?: return
                runCatching { player.play(sampleId, 0.35f, 0.35f, 1, 0, 1f) }
            }

            override fun release() = Unit
        }
    }
}

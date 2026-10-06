package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import android.content.Context
import io.github.rojhatince47.weatherchan.core.common.dispatcher.IoDispatcher
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySuggestionProvider
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * `assets/suggestions.json` dosyasını okur.
 *
 * Veri **gömülüdür**: dosya değişmez, okuma bir kez yapılıp bellekte tutulur.
 * Başarısızlık önbelleğe yazılmaz — dosya sonraki açılışta tekrar denenir
 * (geliştirme sırasında eklenen veri hemen görünür).
 */
@Singleton
class AssetCitySuggestionProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val languageProvider: () -> String
) : CitySuggestionProvider {

    private val mutex = Mutex()

    @Volatile
    private var cachedLanguage: String? = null

    @Volatile
    private var cached: SuggestionData? = null

    override suspend fun load(): SuggestionData? {
        val currentLang = languageProvider()
        cached?.let { if (cachedLanguage == currentLang) return it }
        return mutex.withLock {
            cached?.let { if (cachedLanguage == currentLang) return@withLock it }
            // İptal yutulmaz (proje kuralı, bkz. runCatchingApp): çağıran
            // coroutine iptal edildiyse "veri yok" değil iptal olarak yayılır.
            val data = withContext(ioDispatcher) {
                val raw = runCatching {
                    context.assets.open(FILE_NAME).bufferedReader().use { it.readText() }
                }.onFailure { if (it is CancellationException) throw it }
                    .getOrNull()
                val preferEnglish = currentLang == "en"
                SuggestionFileParser.parse(raw, preferEnglish = preferEnglish)
            }
            data.also {
                if (it != null) {
                    cached = it
                    cachedLanguage = currentLang
                }
            }
        }
    }

    private companion object {
        const val FILE_NAME = "suggestions.json"
    }
}

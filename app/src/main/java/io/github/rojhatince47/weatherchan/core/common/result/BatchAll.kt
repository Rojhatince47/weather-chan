package io.github.rojhatince47.weatherchan.core.common.result

import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.error.ErrorMapper
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Bir toplu isteği, **tek bir öğenin başarısızlığının tüm topluluğu
 * düşürmediği** biçimde çalıştırır.
 *
 * Gerçekte bir sağlayıcı bazı koordinatları hız sınırı (HTTP 429) ya da zaman
 * aşımıyla reddedebilir. `awaitAll` tek hatada bütün topluluğu kaybettirdiği
 * için kullanıcı, elinde çalışan veri olmasına rağmen hiçbir şehir göremiyordu.
 *
 * Kural:
 * - **en az bir** öğe başarılıysa → [AppResult.Success] (kısmen dolu liste)
 * - **hiçbiri** başarılı değilse → ilk gerçek hata eşlenip [AppResult.Failure]
 *
 * Böylece hata hem yutulmaz (tamamı başarısızsa kullanıcı bilgilendirilir) hem
 * de gereksiz yere abartılmaz (bir öğe hata verse diğerleri ekranda kalır).
 *
 * Coroutine iptali hata değildir: yakalanmaz, yeniden fırlatılır.
 */
suspend fun <T, R> ErrorMapper.batchAll(items: List<T>, fetch: suspend (T) -> R): AppResult<List<R>> {
    if (items.isEmpty()) return AppResult.Success(emptyList())
    return coroutineScope {
        val results = items.map { item -> async { runCatching { fetch(item) } } }.awaitAll()
        val firstFailure = results.firstNotNullOfOrNull { it.exceptionOrNull() }
        // İptal başarısızlık değildir; sessizce yutulursa üst katmanlar
        // iptal edilmiş bir isteğin sonucunu kullanıcıya hata diye gösterir.
        if (firstFailure is CancellationException) throw firstFailure
        val succeeded = results.mapNotNull { it.getOrNull() }
        if (succeeded.isNotEmpty()) {
            AppResult.Success(succeeded)
        } else {
            AppResult.Failure(map(firstFailure ?: IllegalStateException("Toplu istek başarısız")))
        }
    }
}

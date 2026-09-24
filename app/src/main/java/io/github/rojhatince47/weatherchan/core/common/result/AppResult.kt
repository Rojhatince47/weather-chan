package io.github.rojhatince47.weatherchan.core.common.result

import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.error.ErrorMapper
import kotlin.coroutines.cancellation.CancellationException

/**
 * Katmanlar arası taşınan sonuç tipi. Exception'lar data katmanında yakalanır,
 * üst katmanlar yalnızca [AppResult] ile çalışır.
 */
sealed interface AppResult<out T> {
    data class Success<T>(
        val data: T
    ) : AppResult<T>

    data class Failure(
        val error: AppError
    ) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

/**
 * [block]'u çalıştırır, fırlatılan hatayı [ErrorMapper] ile [AppError]'a çevirir.
 * Coroutine iptali yutulmaz, yeniden fırlatılır.
 */
suspend inline fun <T> ErrorMapper.runCatchingApp(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    AppResult.Failure(map(e))
}

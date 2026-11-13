package `in`.sms.tringapps.core.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching] but safe for coroutines: cancellation is rethrown instead of
 * being swallowed into a failed [Result]. Inline, so [block] may call suspend functions.
 */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

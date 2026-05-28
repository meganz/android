package mega.privacy.android.core.coroutine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/**
 * Logs any exception thrown by the upstream flow via [Timber] and then swallows it, allowing
 * collection to complete normally instead of crashing the collector.
 *
 * [CancellationException] is rethrown so coroutine cancellation keeps propagating as expected,
 * rather than being logged as an error and silently absorbed.
 *
 * Intended as a drop-in replacement for `catch { Timber.e(it) }`.
 */
fun <T> Flow<T>.logAndSwallowExceptions(): Flow<T> = catch { throwable ->
    if (throwable is CancellationException) throw throwable
    Timber.e(throwable)
}

/**
 * Logs the failure held by this [Result] via [Timber] and returns the [Result] unchanged so the
 * failure is swallowed by the caller.
 *
 * [CancellationException] is rethrown so coroutine cancellation keeps propagating as expected,
 * rather than being caught by the surrounding `runCatching` and logged as an error.
 *
 * Intended as a drop-in replacement for `onFailure { Timber.e(it) }`. Declared `inline` so that
 * Timber's call-site attribution points at the caller rather than this extension.
 */
inline fun <T> Result<T>.logAndSwallowExceptions(): Result<T> = onFailure { throwable ->
    if (throwable is CancellationException) throw throwable
    Timber.e(throwable)
}

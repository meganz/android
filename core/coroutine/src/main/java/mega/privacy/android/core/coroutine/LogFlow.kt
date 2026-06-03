package mega.privacy.android.core.coroutine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Logs every item emitted by this flow via [Timber] for debugging.
 *
 * Only active in debug builds; in release builds the original flow is returned unchanged so there
 * is no logging or per-item overhead.
 *
 * @param name identifies the flow in the log output.
 * @param transform maps an emitted item to its logged representation. Defaults to [toString], but
 * can be supplied to log only the relevant fields of large items.
 */
fun <T> Flow<T>.logFlow(
    name: String,
    transform: (T) -> String = { it.toString() },
): Flow<T> = if (BuildConfig.DEBUG) {
    onEach { Timber.d("$name emitted: ${transform(it)}") }
} else {
    this
}

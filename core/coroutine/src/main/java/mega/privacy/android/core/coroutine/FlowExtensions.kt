package mega.privacy.android.core.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformWhile

/**
 * Converts a Flow to a StateFlow for UI State that remains active while there are active subscribers.
 *
 * Uses a 5-second timeout with `SharingStarted.WhileSubscribed(5000)`. If all subscribers
 * disappear for more than 5 seconds, the upstream Flow stops collecting.
 * This typically occurs when the UI no longer needs to render or after an ANR.
 *
 * @param scope The CoroutineScope to run the StateFlow in
 * @param initialValue The initial value for the StateFlow
 * @return A StateFlow that mirrors the upstream Flow while subscribed
 *
 * Future improvement: Use context parameter to reduce boilerplate.
 */
fun <T> Flow<T>.asUiStateFlow(
    scope: CoroutineScope,
    initialValue: T,
): StateFlow<T> = stateIn(
    scope = scope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = initialValue
)

/**
 * Take while operator that also returns the first value that matches the predicate.
 *
 * @param T flow type parameter
 * @param predicate only one item not matching this predicate will be returned
 */
inline fun <T> Flow<T>.takeWhileInclusive(crossinline predicate: suspend (T) -> Boolean): Flow<T> =
    transformWhile { value ->
        emit(value)
        predicate(value)
    }

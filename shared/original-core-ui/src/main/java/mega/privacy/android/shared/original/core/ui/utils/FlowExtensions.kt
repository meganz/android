package mega.privacy.android.shared.original.core.ui.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlin.math.sign

/**
 * Maps a flow of offsets to a flow of accumulated offsets. It accumulates the offsets as long as they are in the same direction.
 */
fun Flow<Dp>.accumulateDirectionalScrollOffsets() = this
    .pairwise()
    .map { (oldScroll, newScroll) ->
        (newScroll - oldScroll)
    }
    .scan(0.dp) { oldDelta, newDelta ->
        if (newDelta == 0.dp || oldDelta.value.sign == newDelta.value.sign) {
            oldDelta + newDelta
        } else {
            newDelta
        }
    }

private fun <T> Flow<T>.pairwise(): Flow<Pair<T, T>> = flow {
    var previous: T? = null
    collect { value ->
        previous?.let {
            emit(it to value)
        }
        previous = value
    }
}

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
package mega.privacy.android.shared.original.core.ui.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
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
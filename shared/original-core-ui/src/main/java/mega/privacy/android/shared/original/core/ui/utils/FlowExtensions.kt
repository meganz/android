package mega.privacy.android.shared.original.core.ui.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlin.math.sign

/**
 * Creates a flow for the offset of this state. It's expected to have not fully accurate values as lazy list doesn't really know the global offset. It should be used to understand the scroll direction.
 */
fun LazyListState.scrollOffsetFlow() = snapshotFlow {
    firstVisibleItemIndex *
            (layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1) +
            firstVisibleItemScrollOffset
}

/**
 * Creates a flow for the offset of this state. It's expected to have not fully accurate values as lazy grid doesn't really know the global offset. It should be used to understand the scroll direction.
 */
fun LazyGridState.scrollOffsetFlow() = snapshotFlow {
    firstVisibleItemIndex *
            (layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 1) +
            firstVisibleItemScrollOffset
}

/**
 * Creates a flow for the offset of this state.
 */
fun ScrollState.scrollOffsetFlow() = snapshotFlow { value }

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
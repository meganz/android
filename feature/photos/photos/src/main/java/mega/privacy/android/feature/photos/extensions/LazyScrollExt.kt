package mega.privacy.android.feature.photos.extensions

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow

@Composable
internal fun LazyGridState.isScrollingDown(): State<Boolean> =
    rememberScrollingDown(this, { firstVisibleItemIndex }, { firstVisibleItemScrollOffset })

@Composable
internal fun LazyListState.isScrollingDown(): State<Boolean> =
    rememberScrollingDown(this, { firstVisibleItemIndex }, { firstVisibleItemScrollOffset })

/**
 * Observes the first-visible item's index and scroll offset via [snapshotFlow] and reports whether the
 * list is scrolling down. Kept side-effect free (the previous position lives in the collector, not in a
 * [derivedStateOf]).
 *
 * @param key the scroll state the observation is bound to; the observation restarts if it changes.
 */
@Composable
private fun rememberScrollingDown(
    key: Any,
    index: () -> Int,
    scrollOffset: () -> Int,
): State<Boolean> {
    val isScrollingDown = remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        var previousIndex = index()
        var previousScrollOffset = scrollOffset()
        snapshotFlow { index() to scrollOffset() }
            .collect { (currentIndex, currentScrollOffset) ->
                isScrollingDown.value = if (previousIndex != currentIndex) {
                    previousIndex < currentIndex
                } else {
                    previousScrollOffset < currentScrollOffset
                }
                previousIndex = currentIndex
                previousScrollOffset = currentScrollOffset
            }
    }
    return isScrollingDown
}

@Composable
internal fun LazyGridState.isScrolledToEnd(): State<Boolean> = remember(this) {
    derivedStateOf {
        layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
    }
}

@Composable
internal fun LazyListState.isScrolledToEnd(): State<Boolean> = remember(this) {
    derivedStateOf {
        layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
    }
}

@Composable
internal fun LazyGridState.isScrolledToTop(): State<Boolean> = remember(this) {
    derivedStateOf {
        firstVisibleItemIndex <= 1 && firstVisibleItemScrollOffset == 0
    }
}

@Composable
internal fun LazyListState.isScrolledToTop(): State<Boolean> = remember(this) {
    derivedStateOf {
        firstVisibleItemIndex <= 1 && firstVisibleItemScrollOffset == 0
    }
}

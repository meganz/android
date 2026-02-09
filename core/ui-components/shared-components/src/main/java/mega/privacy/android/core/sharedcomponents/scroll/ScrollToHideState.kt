package mega.privacy.android.core.sharedcomponents.scroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates and remembers a [ScrollToHideState] instance to hide UI components
 * like tabs and FAB when scrolling down, and reveal on scroll up.
 *
 * @param scrollDownThreshold The scroll distance required to hide UI components when scrolling down. Default is 60.dp
 * @param scrollUpThreshold The scroll distance required to show UI components when scrolling up. Default is 10.dp
 * @return A remembered [ScrollToHideState] instance
 */
@Composable
fun rememberScrollToHideState(
    scrollDownThreshold: Dp = 60.dp,
    scrollUpThreshold: Dp = 10.dp,
): ScrollToHideState {
    val density = LocalDensity.current
    val scrollDownThresholdPx = with(density) { scrollDownThreshold.toPx() }
    val scrollUpThresholdPx = with(density) { scrollUpThreshold.toPx() }
    return rememberSaveable(
        saver = ScrollToHideState.Saver(scrollDownThresholdPx, scrollUpThresholdPx)
    ) {
        ScrollToHideState(scrollDownThresholdPx, scrollUpThresholdPx)
    }
}

/**
 * Modifier extension to apply scroll-to-hide behavior to a scrollable composable.
 *
 * @param state The [ScrollToHideState] that tracks scroll gestures
 * @return A [Modifier] with nested scroll connection applied
 */
fun Modifier.scrollToHide(state: ScrollToHideState): Modifier =
    this.nestedScroll(state.nestedScrollConnection)

/**
 * State holder for scroll-to-hide functionality.
 *
 * This state tracks vertical scroll gestures and determines whether UI components like tabs and FAB should be hidden
 * based on configurable thresholds. It provides a [NestedScrollConnection] that can be
 * applied to scrollable content.
 *
 * @param scrollDownThresholdPx The scroll down distance in pixels required to hide
 * @param scrollUpThresholdPx The scroll up distance in pixels required to show
 * @param initialShouldHide Initial value for shouldHide state
 */
@Stable
class ScrollToHideState(
    private val scrollDownThresholdPx: Float,
    private val scrollUpThresholdPx: Float,
    initialShouldHide: Boolean = false,
) {
    private var accumulatedScroll = 0f
    var shouldHide by mutableStateOf(initialShouldHide)
        private set

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            val delta = consumed.y

            // Reset accumulated scroll when direction changes
            if ((delta < 0 && accumulatedScroll > 0) || (delta > 0 && accumulatedScroll < 0)) {
                accumulatedScroll = 0f
            }

            accumulatedScroll += delta

            if (accumulatedScroll < -scrollDownThresholdPx && !shouldHide) {
                shouldHide = true
            } else if (accumulatedScroll > scrollUpThresholdPx && shouldHide) {
                shouldHide = false
            }

            // If UI components are hidden and scroll position is at top (unconsumed upward scroll), show them immediately
            if (shouldHide && available.y > 0) {
                shouldHide = false
                accumulatedScroll = 0f
            }

            return Offset.Zero
        }
    }

    /**
     * Resets the scroll state and shows the the UI components
     */
    fun show() {
        accumulatedScroll = 0f
        shouldHide = false
    }

    companion object {
        fun Saver(
            scrollDownThresholdPx: Float,
            scrollUpThresholdPx: Float,
        ): Saver<ScrollToHideState, Boolean> = Saver(
            save = { it.shouldHide },
            restore = { ScrollToHideState(scrollDownThresholdPx, scrollUpThresholdPx, it) }
        )
    }
}


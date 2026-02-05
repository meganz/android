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
 * Creates and remembers a [ScrollToHideTabsState] instance that survives configuration changes.
 *
 * @param hideThreshold The scroll distance required to hide tabs when scrolling down. Default is 60.dp
 * @param showThreshold The scroll distance required to show tabs when scrolling up. Default is 10.dp
 * @return A remembered [ScrollToHideTabsState] instance
 */
@Composable
fun rememberScrollToHideTabsState(
    hideThreshold: Dp = 60.dp,
    showThreshold: Dp = 10.dp,
): ScrollToHideTabsState {
    val density = LocalDensity.current
    val hideThresholdPx = with(density) { hideThreshold.toPx() }
    val showThresholdPx = with(density) { showThreshold.toPx() }
    return rememberSaveable(
        saver = ScrollToHideTabsState.Saver(hideThresholdPx, showThresholdPx)
    ) {
        ScrollToHideTabsState(hideThresholdPx, showThresholdPx)
    }
}

/**
 * Modifier extension to apply scroll-to-hide tabs behavior to a scrollable composable.
 *
 * @param state The [ScrollToHideTabsState] that tracks scroll gestures
 * @return A [Modifier] with nested scroll connection applied
 */
fun Modifier.scrollToHideTabs(state: ScrollToHideTabsState): Modifier =
    this.nestedScroll(state.nestedScrollConnection)

/**
 * State holder for scroll-to-hide tabs functionality.
 *
 * This state tracks vertical scroll gestures and determines whether tabs should be hidden
 * based on configurable thresholds. It provides a [NestedScrollConnection] that can be
 * applied to scrollable content.
 *
 * @param hideThresholdPx The scroll distance in pixels required to hide tabs (scroll down)
 * @param showThresholdPx The scroll distance in pixels required to show tabs (scroll up)
 * @param initialShouldHideTabs Initial value for shouldHideTabs state
 */
@Stable
class ScrollToHideTabsState(
    private val hideThresholdPx: Float,
    private val showThresholdPx: Float,
    initialShouldHideTabs: Boolean = false,
) {
    private var accumulatedScroll = 0f
    var shouldHideTabs by mutableStateOf(initialShouldHideTabs)
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

            if (accumulatedScroll < -hideThresholdPx && !shouldHideTabs) {
                shouldHideTabs = true
            } else if (accumulatedScroll > showThresholdPx && shouldHideTabs) {
                shouldHideTabs = false
            }

            // If tabs are hidden and scroll position is at top (unconsumed upward scroll),
            // show tabs immediately
            if (shouldHideTabs && available.y > 0) {
                shouldHideTabs = false
                accumulatedScroll = 0f
            }

            return Offset.Zero
        }
    }

    /**
     * Resets the scroll state and shows the tabs.
     */
    fun showTabs() {
        accumulatedScroll = 0f
        shouldHideTabs = false
    }

    companion object {
        fun Saver(
            hideThresholdPx: Float,
            showThresholdPx: Float,
        ): Saver<ScrollToHideTabsState, Boolean> = Saver(
            save = { it.shouldHideTabs },
            restore = { ScrollToHideTabsState(hideThresholdPx, showThresholdPx, it) }
        )
    }
}


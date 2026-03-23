package mega.privacy.android.feature.texteditor.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.feature.texteditor.R

private val thumbHeight = 40.dp
private const val HIDE_DELAY_MILLIS = 900

/** Horizontal offset from the right edge so the handle is not flush to the screen edge (matches core-ui scrollbar). */
private val thumbEndPadding = 8.dp

/** Vertical inset at each end of the track so the thumb never sits flush against the toolbar or bottom edge. */
private val thumbTrackVerticalInset = 4.dp

/** Minimum chunk count (not line count) to show the scrollbar. */
private const val MINIMUM_ITEMS_FOR_SCROLLBAR = 50

/** Step size for scrolling large lists; avoids hitting platform scroll-offset limit in one jump. */
private const val SCROLL_STEP_ITEMS = 25_000

/** Delay between scroll steps (ms) so layout can settle and we can scroll further. */
private const val SCROLL_STEP_DELAY_MS = 16L

/**
 * Fast-scroll bar for the text editor LazyColumn.
 *
 * - Uses proportion = firstVisibleItemIndex / itemCount (with continuous offset) so the bar
 *   never shows 100% until the list is actually at the end.
 * - For large lists, [scrollToItem] is done in steps to get past platform scroll limits (~172k
 *   when scroll offset in pixels overflows Int); each step scrolls a batch of items then yields.
 * - Incorporates [LazyListState.firstVisibleItemScrollOffset] for smooth thumb updates on scroll.
 * - Positions the thumb with [thumbEndPadding] from the right edge (matches core-ui scrollbar).
 * - Uses the lambda form of [Modifier.offset] so thumb position updates run in the layout phase
 *   without triggering recomposition on every scroll frame.
 */
@Composable
internal fun TextEditorFastScrollbar(
    state: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    tooltipText: ((currentIndex: Int) -> String)? = null,
) {
    if (itemCount < MINIMUM_ITEMS_FOR_SCROLLBAR) return

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thumbHeightPixels = remember(density) { with(density) { thumbHeight.toPx() } }

    var scrollableHeightPixels by remember { mutableFloatStateOf(0f) }

    var thumbPressed by remember { mutableStateOf(false) }

    // Plain mutable holder so assigning a new job does not trigger recomposition.
    val scrollJobHolder = remember { object { var job: Job? = null } }

    // Drag state: captures scroll proportion at drag-start and accumulates raw deltas.
    // Plain object (not MutableState) to avoid recomposition on updates.
    val dragState = remember {
        object {
            var startProportion = 0f
            var accumulatedPx = 0f
        }
    }

    // itemCount is a non-State parameter read directly inside the derivedStateOf body, so it
    // must be a remember key to keep the lambda up to date when the chunk count changes.
    val scrollProportion by remember(state, itemCount) {
        derivedStateOf {
            val visibleItems = state.layoutInfo.visibleItemsInfo
            calculateScrollProportion(
                firstVisibleItemIndex = state.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = state.firstVisibleItemScrollOffset,
                lastVisibleItemIndex = visibleItems.lastOrNull()?.index,
                firstVisibleItemSize = visibleItems.firstOrNull()?.size?.toFloat(),
                itemCount = itemCount,
            )
        }
    }

    // thumbPressed is a mutableState — observable inside derivedStateOf, no need for it as a key.
    val thumbVisible by remember(state, itemCount) {
        derivedStateOf {
            itemCount > 0 &&
                (state.isScrollInProgress || state.canScrollForward || state.canScrollBackward || thumbPressed)
        }
    }

    // rememberUpdatedState wraps the lambda so it stays current without being a remember key.
    val latestTooltipText = rememberUpdatedState(tooltipText)
    val tooltipString by remember(state, itemCount) {
        derivedStateOf {
            if (thumbVisible) latestTooltipText.value?.invoke(state.firstVisibleItemIndex) else null
        }
    }

    val enterAnimation = remember {
        fadeIn() + scaleIn(
            transformOrigin = TransformOrigin(1f, 0.5f),
            initialScale = 0.5f,
        )
    }
    val exitAnimation = remember {
        scaleOut(
            animationSpec = tween(delayMillis = HIDE_DELAY_MILLIS),
            targetScale = 0.5f,
            transformOrigin = TransformOrigin(1f, 0.5f),
        ) + fadeOut(animationSpec = tween(delayMillis = HIDE_DELAY_MILLIS))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(
                top = thumbTrackVerticalInset,
                bottom = thumbTrackVerticalInset,
                end = thumbEndPadding,
            )
            .onGloballyPositioned { coordinates ->
                scrollableHeightPixels = (coordinates.size.height - thumbHeightPixels)
                    .toFloat().coerceAtLeast(0f)
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbHeight)
                // Lambda form: state reads deferred to layout phase — no recomposition on scroll.
                .offset {
                    IntOffset(
                        x = 0,
                        y = (scrollableHeightPixels * scrollProportion).roundToInt(),
                    )
                },
            contentAlignment = Alignment.CenterEnd,
        ) {
            AnimatedVisibility(
                visible = thumbVisible,
                enter = enterAnimation,
                exit = exitAnimation,
            ) {
                Surface(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    thumbPressed = true
                                    if (tryAwaitRelease()) thumbPressed = false
                                },
                            )
                        }
                        .pointerInput(itemCount, state) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    thumbPressed = true
                                    dragState.startProportion = scrollProportion
                                    dragState.accumulatedPx = 0f
                                },
                                onDragEnd = { thumbPressed = false },
                                onDragCancel = { thumbPressed = false },
                            ) { change, dragAmount ->
                                if (scrollableHeightPixels > 0 && itemCount > 0) {
                                    change.consume()
                                    dragState.accumulatedPx += dragAmount
                                    val rawProportion = dragState.startProportion + dragState.accumulatedPx / scrollableHeightPixels
                                    val dragProportion = rawProportion.coerceIn(0f, 1f)
                                    val targetIndex = (dragProportion * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)
                                    scrollJobHolder.job?.cancel()
                                    scrollJobHolder.job = coroutineScope.launch {
                                        var current = state.firstVisibleItemIndex
                                        if (targetIndex <= current + SCROLL_STEP_ITEMS) {
                                            state.scrollToItem(targetIndex.coerceIn(0, itemCount - 1))
                                        } else {
                                            var next = (current + SCROLL_STEP_ITEMS).coerceAtMost(targetIndex)
                                            var prevIndex = -1
                                            while (next < targetIndex) {
                                                state.scrollToItem(next.coerceIn(0, itemCount - 1))
                                                delay(SCROLL_STEP_DELAY_MS)
                                                current = state.firstVisibleItemIndex
                                                // No progress means scrollToItem hit platform limits; bail to avoid infinite loop.
                                                if (current >= targetIndex || current == prevIndex) break
                                                prevIndex = current
                                                next = (current + SCROLL_STEP_ITEMS).coerceAtMost(targetIndex)
                                            }
                                            state.scrollToItem(targetIndex.coerceIn(0, itemCount - 1))
                                        }
                                    }
                                }
                            }
                        }
                        .size(thumbHeight),
                    shape = RoundedCornerShape(size = 56.dp),
                    color = DSTokens.colors.background.surface1,
                    shadowElevation = 8.dp,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_triangle_up_down_small_regular),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }

            tooltipString?.let { text ->
                AnimatedVisibility(
                    visible = thumbPressed,
                    enter = enterAnimation,
                    exit = exitAnimation,
                ) {
                    Text(
                        text = text,
                        modifier = Modifier
                            .offset(x = (-40).dp)
                            .padding(end = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/**
 * Calculates the 0..1 scroll proportion for the fast-scrollbar thumb.
 *
 * Returns 1f when the last visible item is the final item in the list. Otherwise computes a
 * continuous proportion that incorporates the sub-item pixel offset for smooth thumb tracking.
 *
 * @param firstVisibleItemIndex  Index of the first fully or partially visible item.
 * @param firstVisibleItemScrollOffset  Pixel offset of the first visible item from the top of the viewport.
 * @param lastVisibleItemIndex  Index of the last visible item, or null if the layout has no items yet.
 * @param firstVisibleItemSize  Height in pixels of the first visible item, or null if unavailable.
 * @param itemCount  Total number of items in the list.
 */
internal fun calculateScrollProportion(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    lastVisibleItemIndex: Int?,
    firstVisibleItemSize: Float?,
    itemCount: Int,
): Float {
    val itemCountFloat = itemCount.toFloat().coerceAtLeast(1f)
    val lastIndex = lastVisibleItemIndex ?: firstVisibleItemIndex
    return if (lastIndex >= itemCount - 1) 1f
    else {
        val itemSize = (firstVisibleItemSize ?: 1f).coerceAtLeast(1f)
        val itemProgress = firstVisibleItemScrollOffset / itemSize
        val continuousIndex = firstVisibleItemIndex.toFloat() + itemProgress
        (continuousIndex / itemCountFloat).coerceIn(0f, 1f)
    }
}

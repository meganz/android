package mega.privacy.android.feature.texteditor.components

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
import mega.android.core.ui.tokens.theme.DSTokens
import kotlin.math.roundToInt

private val thumbHeight = 40.dp
private const val HIDE_DELAY_MILLIS = 900

/** Horizontal offset from the right edge so the handle is not flush to the screen edge (matches core-ui scrollbar). */
private val thumbEndPadding = 8.dp

/** Vertical inset at each end of the track so the thumb never sits flush against the toolbar or bottom edge. */
private val thumbTrackVerticalInset = 4.dp

/**
 * Minimum chunk count to compose the scrollbar at all. A single chunk can still span many screens
 * (view mode caps chunks at ~50k chars, so e.g. an XML file under that size is one tall chunk), so
 * this only filters out empty content; whether the thumb is actually visible is decided by
 * [LazyListState.canScrollForward]/[LazyListState.canScrollBackward] via `thumbVisible`.
 */
private const val MINIMUM_ITEMS_FOR_SCROLLBAR = 1

/**
 * Fast-scroll bar for the text editor LazyColumn.
 *
 * - Uses proportion = firstVisibleItemIndex / itemCount (with continuous offset) so the bar
 *   never shows 100% until the list is actually at the end.
 * - While dragging, the thumb and tooltip follow the finger directly (via the live drag target),
 *   not the settled scroll state, so they track 1:1 and don't jitter while a heavy chunk remeasures.
 *   On release both resume reflecting the real scroll position.
 * - A drag scrolls via [LazyListState.requestScrollToItem]: synchronous and coalescing, so rapid
 *   drag deltas overwrite the pending request instead of cancelling a scroll coroutine per frame,
 *   and snapping by index sidesteps the platform pixel-scroll limit.
 * - Incorporates [LazyListState.firstVisibleItemScrollOffset] for smooth thumb updates on scroll.
 * - Positions the thumb with [thumbEndPadding] from the right edge (matches core-ui scrollbar).
 * - Uses the lambda form of [Modifier.offset] so thumb position updates run in the layout phase
 *   without triggering recomposition on every scroll frame.
 */
@Composable
fun TextEditorFastScrollbar(
    state: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    tooltipText: ((chunkIndex: Int, fractionWithinChunk: Float) -> String)? = null,
) {
    if (!shouldShowScrollbar(itemCount)) return

    val density = LocalDensity.current

    val thumbHeightPixels = remember(density) { with(density) { thumbHeight.toPx() } }

    var scrollableHeightPixels by remember { mutableFloatStateOf(0f) }

    var thumbPressed by remember { mutableStateOf(false) }

    // While dragging, holds the finger's proportion so the thumb tracks it directly; null otherwise,
    // falling back to the real scroll proportion. Read in the offset lambda (layout phase).
    var dragProportion by remember { mutableStateOf<Float?>(null) }

    // While dragging, holds the chunk + fraction the finger is scrubbing to, so the tooltip leads the
    // finger instead of trailing the settled scroll position; null otherwise.
    var dragTooltipTarget by remember { mutableStateOf<DragTooltipTarget?>(null) }

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
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            calculateScrollProportion(
                firstVisibleItemIndex = state.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = state.firstVisibleItemScrollOffset,
                firstVisibleItemSize = visibleItems.firstOrNull()?.size?.toFloat(),
                itemCount = itemCount,
                viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset,
                canScrollForward = state.canScrollForward,
                canScrollBackward = state.canScrollBackward,
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
            if (thumbVisible) {
                val dragTarget = dragTooltipTarget
                if (dragTarget != null) {
                    // Mid-drag: report where the finger is scrubbing to.
                    latestTooltipText.value?.invoke(
                        dragTarget.chunkIndex,
                        dragTarget.fractionWithinChunk
                    )
                } else {
                    // Fraction the first visible chunk has scrolled past the top, so the caller can
                    // interpolate the top line within the chunk rather than its (fixed) start line.
                    val size =
                        state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 0f
                    val fractionWithinChunk =
                        if (size > 0f) (state.firstVisibleItemScrollOffset / size).coerceIn(
                            0f,
                            1f
                        ) else 0f
                    latestTooltipText.value?.invoke(
                        state.firstVisibleItemIndex,
                        fractionWithinChunk
                    )
                }
            } else null
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
                // During a drag the thumb follows the finger; otherwise the real scroll position.
                .offset {
                    val proportion = dragProportion ?: scrollProportion
                    IntOffset(
                        x = 0,
                        y = (scrollableHeightPixels * proportion).roundToInt(),
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
                                    dragProportion = scrollProportion
                                },
                                onDragEnd = {
                                    thumbPressed = false
                                    dragProportion = null
                                    dragTooltipTarget = null
                                },
                                onDragCancel = {
                                    thumbPressed = false
                                    dragProportion = null
                                    dragTooltipTarget = null
                                },
                            ) { change, dragAmount ->
                                if (scrollableHeightPixels > 0 && itemCount > 0) {
                                    change.consume()
                                    dragState.accumulatedPx += dragAmount
                                    val proportion = (dragState.startProportion +
                                            dragState.accumulatedPx / scrollableHeightPixels).coerceIn(
                                        0f,
                                        1f
                                    )
                                    // Drive the thumb from the finger so it tracks 1:1; the content
                                    // catches up via the request below. Decoupling them removes the
                                    // multi-chunk jitter.
                                    dragProportion = proportion
                                    // Continuous position within the list (index + sub-item fraction)
                                    // so the content tracks the finger instead of snapping to chunks.
                                    val target = calculateScrollTarget(proportion, itemCount)
                                    // Approximate the target chunk height with the first visible
                                    // chunk's; chunks are near-uniform, close enough for fast-scroll.
                                    val chunkSizePx = state.layoutInfo.visibleItemsInfo
                                        .firstOrNull()?.size ?: 0
                                    val viewportSizePx = state.layoutInfo.viewportEndOffset -
                                            state.layoutInfo.viewportStartOffset
                                    val scrollOffset = calculateScrollOffset(
                                        offsetFraction = target.offsetFraction,
                                        itemSizePx = chunkSizePx,
                                        viewportSizePx = viewportSizePx,
                                        isLastItem = target.index == itemCount - 1,
                                    )
                                    // Feed the tooltip the same target, size-normalised to match the
                                    // real-scroll path so the label doesn't jump when the drag ends.
                                    dragTooltipTarget = DragTooltipTarget(
                                        chunkIndex = target.index,
                                        fractionWithinChunk = if (chunkSizePx > 0) {
                                            (scrollOffset.toFloat() / chunkSizePx).coerceIn(0f, 1f)
                                        } else 0f,
                                    )
                                    // Synchronous, coalescing snap on next remeasure: rapid deltas just
                                    // overwrite the pending request; index snapping sidesteps the pixel limit.
                                    state.requestScrollToItem(target.index, scrollOffset)
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
                            tint = DSTokens.colors.icon.secondary,
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
                        color = DSTokens.colors.text.primary,
                    )
                }
            }
        }
    }
}

/**
 * Calculates the 0..1 scroll proportion for the fast-scrollbar thumb.
 *
 * The end of the track is pinned to the *actual* bottom of the list ([canScrollForward] == false),
 * not to the last item merely becoming visible. With very tall chunks — e.g. a JSON file whose single
 * long line is split into 50k-char chunks that each wrap over many screens — the last chunk appears a
 * screen or more before the real bottom; keying on its visibility made the thumb jump to the end and
 * stop following the finger (AND-23767 / T21378947). Otherwise the proportion is continuous, using the
 * first visible item's sub-item pixel offset so the thumb tracks the scroll smoothly.
 *
 * @param firstVisibleItemIndex  Index of the first fully or partially visible item.
 * @param firstVisibleItemScrollOffset  Pixel offset of the first visible item from the top of the viewport.
 * @param firstVisibleItemSize  Height in pixels of the first visible item, or null if unavailable.
 * @param itemCount  Total number of items in the list.
 * @param viewportSize  Main-axis size of the viewport in pixels (its scrollable extent).
 * @param canScrollForward  Whether the list can still scroll down; false only at the true bottom.
 * @param canScrollBackward  Whether the list can scroll up; false only at the true top.
 */
internal fun calculateScrollProportion(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    firstVisibleItemSize: Float?,
    itemCount: Int,
    viewportSize: Int,
    canScrollForward: Boolean,
    canScrollBackward: Boolean,
): Float {
    if (itemCount <= 0) return 0f
    // Pin to the ends only when the list is genuinely there. A non-scrollable list (content fits the
    // viewport) can scroll neither way — keep the thumb at the top rather than snapping it to the end.
    if (!canScrollForward) return if (canScrollBackward) 1f else 0f
    val itemCountFloat = itemCount.toFloat().coerceAtLeast(1f)
    val itemSize = (firstVisibleItemSize ?: 1f).coerceAtLeast(1f)
    // While the first visible item is also the last item, it can only scroll until its tail reaches the
    // viewport bottom — its remaining travel is (itemSize - viewport), not the full itemSize. Dividing by
    // the full itemSize leaves a viewport-sized gap below the thumb that never closes until canScrollForward
    // flips at the true bottom, which then snaps the thumb straight to 1f. With few chunks that gap is large
    // — most visibly a single wrapped line (one tall chunk), where the thumb sits near the middle a screen
    // from the end and then jumps to the bottom (and back when scrolling up again). Normalising the last
    // item by its real travel keeps the thumb continuous all the way to the end (AND-23767 / T21378947).
    val isLastItem = firstVisibleItemIndex >= itemCount - 1
    val itemTravel = if (isLastItem) (itemSize - viewportSize).coerceAtLeast(1f) else itemSize
    val itemProgress = (firstVisibleItemScrollOffset / itemTravel).coerceIn(0f, 1f)
    val continuousIndex = firstVisibleItemIndex.toFloat() + itemProgress
    return (continuousIndex / itemCountFloat).coerceIn(0f, 1f)
}

/**
 * Where the finger is scrubbing to during a drag, used to drive the tooltip line label.
 *
 * @param chunkIndex  The target chunk (item) index.
 * @param fractionWithinChunk  How far into that chunk, size-normalised (offset / chunk size, 0f..1f) to
 *   match the real-scroll tooltip path so the label doesn't jump when the drag ends.
 */
private data class DragTooltipTarget(
    val chunkIndex: Int,
    val fractionWithinChunk: Float,
)

/**
 * Target position for a fast-scroll drag: an item [index] plus the fraction of the way [offsetFraction]
 * (0..1) the thumb sits into that item. The fraction is later converted to a pixel scroll offset using
 * the measured item height so the thumb tracks the finger continuously rather than snapping per item.
 *
 * @param index  The item to scroll to (0-based, clamped to the list bounds).
 * @param offsetFraction  How far into [index] to scroll, in the range 0f..1f.
 */
internal data class ScrollTarget(
    val index: Int,
    val offsetFraction: Float,
)

/**
 * Maps a 0..1 drag [proportion] onto a continuous scroll position in a list of [itemCount] items.
 *
 * Because each item (text chunk) spans up to a thousand lines, snapping the drag to whole item
 * indices makes the thumb feel coarse and jumpy. Treating the drag as a position over the whole
 * line range — item index plus a sub-item fraction — keeps the gesture smooth. The fraction is later
 * turned into a pixel offset inside the target item.
 *
 * @param proportion  The drag proportion along the track, in the range 0f..1f.
 * @param itemCount  Total number of items in the list.
 */
internal fun calculateScrollTarget(proportion: Float, itemCount: Int): ScrollTarget {
    if (itemCount <= 0) return ScrollTarget(index = 0, offsetFraction = 0f)
    val clamped = proportion.coerceIn(0f, 1f)
    val continuousPosition = clamped * itemCount
    val index = continuousPosition.toInt().coerceIn(0, itemCount - 1)
    val offsetFraction = (continuousPosition - index).coerceIn(0f, 1f)
    return ScrollTarget(index = index, offsetFraction = offsetFraction)
}

/**
 * Converts a drag [offsetFraction] within the target chunk into a pixel scroll offset, mirroring the
 * last-chunk travel normalisation in [calculateScrollProportion].
 *
 * The last (and single) chunk only scrolls by `itemSize - viewport` before the list bottoms out, so
 * mapping onto the full [itemSizePx] would saturate the offset early — scrolling small/last chunks too
 * fast and leaving a dead zone at the track's end. Non-last chunks use their full height. Keeping this
 * the exact inverse of [calculateScrollProportion] is what makes the thumb track the finger 1:1.
 *
 * @param offsetFraction  How far into the target chunk the thumb sits, in the range 0f..1f.
 * @param itemSizePx  Measured height of the target chunk in pixels.
 * @param viewportSizePx  Main-axis size of the viewport in pixels.
 * @param isLastItem  Whether the target chunk is the last item in the list.
 */
internal fun calculateScrollOffset(
    offsetFraction: Float,
    itemSizePx: Int,
    viewportSizePx: Int,
    isLastItem: Boolean,
): Int {
    val travelPx = (if (isLastItem) itemSizePx - viewportSizePx else itemSizePx).coerceAtLeast(0)
    return (offsetFraction.coerceIn(0f, 1f) * travelPx).roundToInt()
}

/**
 * Whether the scrollbar should be composed for the given [itemCount] (chunk count, not line count).
 * Returns true for any non-empty list — even a single chunk, which can still span many screens. The
 * thumb only becomes visible when the list is actually scrollable (see `thumbVisible`).
 */
internal fun shouldShowScrollbar(itemCount: Int): Boolean =
    itemCount >= MINIMUM_ITEMS_FOR_SCROLLBAR

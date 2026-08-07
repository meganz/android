package mega.privacy.android.feature.photos.extensions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

fun Modifier.photosZoomGestureDetector(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
) = this.pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent(
                pass = PointerEventPass.Initial
            )
            if (event.changes.any { it.isConsumed })
                break
            val zoomChange = event.calculateZoom()
            if (zoomChange != 1.0f) {
                if (zoomChange > 1.0f) {
                    onZoomIn()
                } else {
                    onZoomOut()
                }
                // Consume event in case to trigger scroll
                event.changes.map { it.consume() }
                break
            }
        } while (event.changes.any { it.pressed })
    }
}

/**
 * Drag-to-select gesture for a photos grid: long-pressing a media cell anchors the gesture (the
 * cell itself is toggled by its own long-click handler) and dragging spreads the anchor's toggle
 * across every media cell between the anchor and the pointer — dragging from an unselected cell
 * selects the swept range, dragging from a selected cell deselects it. A selecting drag owns every
 * cell it sweeps: retreating deselects cells leaving the range even when they were selected before
 * the gesture. A deselecting drag instead restores cells it retreats from to their pre-drag state,
 * so it never selects anything. Dragging past the top/bottom edge auto-scrolls the grid,
 * frame-synchronised and capped at [DRAG_TO_SELECT_MAX_AUTO_SCROLL_VELOCITY], while keeping the
 * range in sync with the resting pointer.
 *
 * The anchor is tracked by item key and re-resolved on every update, so the range stays anchored
 * to the same photo when the dataset shifts mid-gesture (e.g. an upload prepending items).
 *
 * @param lazyGridState the grid's state, used for pointer hit-testing and auto-scroll
 * @param mediaIndexOfKey resolves a grid item key to its ordinal media index, or null when the
 * item is not a selectable media cell (headers, banners). Must read current data via snapshot
 * state, as the gesture keeps the instance captured when the pointer input starts.
 * @param isMediaSelected whether the media cell at the given ordinal index is currently selected;
 * sampled at the anchor to decide the drag mode, and when a cell enters the range so a deselecting
 * drag can restore it should it leave again
 * @param onDragSelectionChange called with the ordinal media index and the selection state it
 * should take; the caller applies it idempotently through the same event a tap or long-press fires
 */
internal fun Modifier.photosGridDragToSelectGesture(
    lazyGridState: LazyGridState,
    mediaIndexOfKey: (key: Any?) -> Int?,
    isMediaSelected: (mediaIndex: Int) -> Boolean,
    onDragSelectionChange: (mediaIndex: Int, selected: Boolean) -> Unit,
) = this.pointerInput(lazyGridState) {
    val autoScrollThreshold = DRAG_TO_SELECT_AUTO_SCROLL_THRESHOLD.toPx()
    val maxAutoScrollVelocity = DRAG_TO_SELECT_MAX_AUTO_SCROLL_VELOCITY.toPx()
    val autoScrollVelocity = MutableStateFlow(0f)
    var anchorKey: Any? = null
    var anchorReported = false
    var dragSelectMode = true
    var lastHitIndex: Int? = null
    var lastPointerPosition: Offset? = null
    // Media index -> whether the cell was selected before the drag swept it. A cell leaving the
    // range is deselected by a selecting drag, restored to this state by a deselecting one.
    val enteredCells = mutableMapOf<Int, Boolean>()

    fun mediaCellAt(position: Offset): Pair<Any, Int>? =
        lazyGridState.layoutInfo.visibleItemsInfo.find { item ->
            item.size.toIntRect().contains(position.round() - item.offset)
        }?.let { item -> mediaIndexOfKey(item.key)?.let { index -> item.key to index } }

    fun extendSelectionTo(position: Offset) {
        lastPointerPosition = position
        val anchor = mediaIndexOfKey(anchorKey) ?: return
        val hitIndex = mediaCellAt(position)?.second ?: return
        if (hitIndex == lastHitIndex) return
        lastHitIndex = hitIndex
        if (!anchorReported) {
            anchorReported = true
            onDragSelectionChange(anchor, dragSelectMode)
        }
        val range = minOf(anchor, hitIndex)..maxOf(anchor, hitIndex)
        val iterator = enteredCells.iterator()
        while (iterator.hasNext()) {
            val (index, wasSelected) = iterator.next()
            if (index !in range) {
                onDragSelectionChange(index, wasSelected && !dragSelectMode)
                iterator.remove()
            }
        }
        range.forEach { index ->
            if (index != anchor && index !in enteredCells) {
                enteredCells[index] = isMediaSelected(index)
                onDragSelectionChange(index, dragSelectMode)
            }
        }
    }

    fun endDrag() {
        autoScrollVelocity.value = 0f
        anchorKey = null
        anchorReported = false
        dragSelectMode = true
        lastHitIndex = null
        lastPointerPosition = null
        enteredCells.clear()
    }

    coroutineScope {
        launch {
            // One persistent frame loop per scroll episode, reading the live velocity each frame.
            // Collecting velocity values directly would cancel and restart the loop on every
            // pointer move through the hot zone, stalling the scroll while the finger still moves.
            autoScrollVelocity
                .map { it != 0f }
                .distinctUntilChanged()
                .collectLatest { scrolling ->
                    if (!scrolling) return@collectLatest
                    var lastFrameNanos = withFrameNanos { it }
                    while (true) {
                        val frameNanos = withFrameNanos { it }
                        val deltaSeconds = (frameNanos - lastFrameNanos) / 1e9f
                        lastFrameNanos = frameNanos
                        val delta = autoScrollVelocity.value * deltaSeconds
                        // At the scroll boundary idle on frames without touching the scroll state,
                        // so a direction change or newly loaded content resumes scrolling.
                        val canScroll = if (delta > 0) {
                            lazyGridState.canScrollForward
                        } else {
                            lazyGridState.canScrollBackward
                        }
                        if (canScroll && lazyGridState.scrollBy(delta) != 0f) {
                            // Keep extending the selection as cells scroll under the resting pointer.
                            lastPointerPosition?.let(::extendSelectionTo)
                        }
                    }
                }
        }

        // The gesture is tracked on the Initial pass: once a media cell's own combinedClickable
        // fires its long-click it consumes every following event until the pointer is up, which
        // would cancel any Main-pass detector (e.g. detectDragGesturesAfterLongPress). The
        // Initial pass sees each event before the cell (or the grid's scroll) can consume it.
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.isConsumed) return@awaitEachGesture
            val anchorCell = mediaCellAt(down.position) ?: return@awaitEachGesture
            // Sampled before the long press toggles the anchor: its pre-toggle state decides
            // whether this drag selects or deselects the swept range.
            val selectMode = !isMediaSelected(anchorCell.second)

            if (!awaitLongPressOnInitialPass(down)) return@awaitEachGesture

            dragSelectMode = selectMode
            anchorKey = anchorCell.first
            lastHitIndex = anchorCell.second
            lastPointerPosition = down.position
            try {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    // Own the gesture from here: consuming on the Initial pass stops the grid
                    // from scrolling and the cells from click-handling while drag-selecting.
                    event.changes.forEach { it.consume() }
                    if (!change.pressed) break
                    val distanceFromTop = change.position.y
                    val distanceFromBottom = size.height - change.position.y
                    val overshootFraction = when {
                        distanceFromBottom < autoScrollThreshold ->
                            ((autoScrollThreshold - distanceFromBottom) / autoScrollThreshold)
                                .coerceAtMost(1f)

                        distanceFromTop < autoScrollThreshold ->
                            -((autoScrollThreshold - distanceFromTop) / autoScrollThreshold)
                                .coerceAtMost(1f)

                        else -> 0f
                    }
                    autoScrollVelocity.value = overshootFraction * maxAutoScrollVelocity
                    extendSelectionTo(change.position)
                }
            } finally {
                endDrag()
            }
        }
    }
}

/**
 * Waits on the Initial pass for the pointer that went down with [down] to become a long press.
 * Returns false when the gesture ends or turns into something else first: the pointer is lifted
 * (tap), moves beyond the touch slop (scroll), a second pointer joins (zoom), or another handler
 * consumes the gesture.
 */
private suspend fun AwaitPointerEventScope.awaitLongPressOnInitialPass(
    down: PointerInputChange,
): Boolean = try {
    withTimeout(viewConfiguration.longPressTimeoutMillis) {
        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
                ?: return@withTimeout false
            val isCancelled = !change.pressed ||
                    event.changes.size > 1 ||
                    event.changes.any { it.isConsumed } ||
                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop
            if (isCancelled) return@withTimeout false
        }
        @Suppress("UNREACHABLE_CODE")
        return@withTimeout false
    }
} catch (_: PointerEventTimeoutCancellationException) {
    true
}

/** Distance from the top/bottom edge within which dragging starts to auto-scroll the grid. */
private val DRAG_TO_SELECT_AUTO_SCROLL_THRESHOLD = 100.dp

/** Auto-scroll speed per second when the pointer is at (or past) the viewport edge. */
private val DRAG_TO_SELECT_MAX_AUTO_SCROLL_VELOCITY = 1600.dp
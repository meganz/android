package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Draggable selection handles for the cross-block document selection, drawn as an overlay on
 * top of the block list: the classic teardrops under each selection edge, leaning outward like
 * the native ones. Dragging a handle moves that edge of [RichDocumentState.documentSelection]
 * across blocks (edges cannot cross); the text toolbar hides for the duration of the drag.
 */
@Composable
internal fun DocumentSelectionHandles(
    state: RichDocumentState,
    geometry: RichSelectionGeometry,
    color: Color,
    onDragStart: () -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var overlayCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(modifier = modifier.onGloballyPositioned { overlayCoordinates = it }) {
        DocumentSelectionHandle(
            isStart = true,
            state = state,
            geometry = geometry,
            overlayCoordinates = { overlayCoordinates },
            color = color,
            onDragStart = onDragStart,
            onDragStopped = onDragStopped,
        )
        DocumentSelectionHandle(
            isStart = false,
            state = state,
            geometry = geometry,
            overlayCoordinates = { overlayCoordinates },
            color = color,
            onDragStart = onDragStart,
            onDragStopped = onDragStopped,
        )
    }
}

@Composable
private fun DocumentSelectionHandle(
    isStart: Boolean,
    state: RichDocumentState,
    geometry: RichSelectionGeometry,
    overlayCoordinates: () -> LayoutCoordinates?,
    color: Color,
    onDragStart: () -> Unit,
    onDragStopped: () -> Unit,
) {
    val anchor by remember(state, geometry, isStart) {
        derivedStateOf {
            val selection = state.documentSelection ?: return@derivedStateOf null
            val edge = if (isStart) selection.start else selection.end
            geometry.edgeCursorRect(edge)
        }
    }
    val anchorRect = anchor ?: return
    val overlayOrigin = overlayCoordinates()?.takeIf { it.isAttached }
        ?.positionInRoot() ?: return
    var handleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = Modifier
            .offset {
                val local = Offset(anchorRect.left, anchorRect.bottom) - overlayOrigin
                val x = if (isStart) local.x - HandleTouchTarget.toPx() else local.x
                IntOffset(x.roundToInt(), local.y.roundToInt())
            }
            .size(HandleTouchTarget)
            .onGloballyPositioned { handleCoordinates = it }
            .pointerInput(state, geometry, isStart) {
                // The finger is tracked in ROOT coordinates: the handle node itself moves as
                // the selection edge advances (and as autoscroll shifts content beneath a
                // stationary finger), so accumulating local drag deltas would drift.
                fun fingerRoot(position: Offset): Offset? =
                    handleCoordinates?.takeIf { it.isAttached }?.localToRoot(position)

                fun dragTo(root: Offset) {
                    if (state.documentSelection == null) {
                        geometry.autoscroll = null
                        return
                    }
                    // The grab point rides below the line; sample at the line the handle
                    // hangs from rather than under the finger.
                    val sample = root - Offset(0f, HandleTouchTarget.toPx() / 2)
                    geometry.docPositionAt(state, sample)?.let { position ->
                        state.moveSelectionEdge(isStart, position)
                    }
                    geometry.autoscroll = geometry.editorCoordinates?.takeIf { it.isAttached }
                        ?.let { coordinates ->
                            selectionAutoscrollVelocity(coordinates.boundsInRoot(), sample.y)
                                .takeIf { it != 0f }
                                ?.let { velocity ->
                                    SelectionAutoscroll(velocity, sample) { position ->
                                        state.moveSelectionEdge(isStart, position)
                                    }
                                }
                        }
                }

                detectDragGestures(
                    onDragStart = { startPosition ->
                        if (state.documentSelection == null) return@detectDragGestures
                        onDragStart()
                        fingerRoot(startPosition)?.let(::dragTo)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        fingerRoot(change.position)?.let(::dragTo)
                    },
                    onDragEnd = {
                        geometry.autoscroll = null
                        onDragStopped()
                    },
                    onDragCancel = {
                        geometry.autoscroll = null
                        onDragStopped()
                    },
                )
            }
            .testTag(
                if (isStart) RICH_SELECTION_START_HANDLE_TAG else RICH_SELECTION_END_HANDLE_TAG,
            ),
        contentAlignment = if (isStart) Alignment.TopEnd else Alignment.TopStart,
    ) {
        SelectionHandleGlyph(isStart = isStart, color = color)
    }
}

/** The teardrop: a circle with the corner squared off on the side that anchors the text. */
@Composable
private fun SelectionHandleGlyph(isStart: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(HandleVisualSize)) {
        val radius = size.width / 2f
        val path = Path().apply {
            addOval(Rect(0f, 0f, size.width, size.height))
            if (isStart) {
                addRect(Rect(radius, 0f, size.width, radius))
            } else {
                addRect(Rect(0f, 0f, radius, radius))
            }
        }
        drawPath(path, color)
    }
}

const val RICH_SELECTION_START_HANDLE_TAG = "rich_document_editor:selection_handle_start"
const val RICH_SELECTION_END_HANDLE_TAG = "rich_document_editor:selection_handle_end"

private val HandleVisualSize = 22.dp
private val HandleTouchTarget = 44.dp

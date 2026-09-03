package mega.privacy.android.feature.texteditor.components.markdown.rich

import android.content.ClipData
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A live edge-autoscroll request from a selection drag: while set, the editor scrolls the
 * list by [velocity] each frame and keeps extending the selection toward the finger, which
 * is stationary at [dragRoot] while content moves beneath it.
 */
internal class SelectionAutoscroll(
    val velocity: Float,
    val dragRoot: Offset,
    val moveEdge: (RichDocPosition) -> Unit,
)

/** Signed per-frame scroll step for a drag at [y]; zero outside the viewport edge zones. */
internal fun Density.selectionAutoscrollVelocity(viewport: Rect, y: Float): Float {
    val threshold = AutoscrollThreshold.toPx()
    val max = AutoscrollMaxStep.toPx()
    return when {
        y > viewport.bottom - threshold ->
            ((y - (viewport.bottom - threshold)) / threshold).coerceAtMost(1f) * max

        y < viewport.top + threshold ->
            -(((viewport.top + threshold) - y) / threshold).coerceAtMost(1f) * max

        else -> 0f
    }
}

/**
 * Live geometry of the rich editor's blocks, in root coordinates, registered by the block rows
 * and read by the cross-block selection gesture to map touch positions to
 * [RichDocPosition]s. Entries only exist for blocks currently composed by the lazy list, which
 * is exactly the set a drag can reach. Snapshot-state maps so the selection handles, which
 * derive their placement from these bounds, track the content through scrolling.
 */
internal class RichSelectionGeometry {
    var editorCoordinates: LayoutCoordinates? = null
    var autoscroll: SelectionAutoscroll? by mutableStateOf(null)
    val fieldBounds = mutableStateMapOf<Int, Rect>()

    /**
     * The fields' text layout providers, as handed to `onTextLayout`. That callback runs inside
     * the field's measure pass, where a snapshot-state write re-dirties layout every frame and
     * Compose never goes idle — so this is a plain map holding the provider, resolved lazily.
     * Resolving reads the field's own layout state, which the handles' derived placement
     * observes just the same.
     */
    val textLayoutProviders = mutableMapOf<Int, () -> TextLayoutResult?>()

    /** Raw-source rows: they carry their own [SelectionContainer], so the gesture stays out. */
    val rawBounds = mutableStateMapOf<Int, Rect>()

    fun clear(index: Int) {
        fieldBounds.remove(index)
        textLayoutProviders.remove(index)
        rawBounds.remove(index)
    }

    private fun textLayoutAt(index: Int): TextLayoutResult? =
        textLayoutProviders[index]?.invoke()

    fun rootPositionOf(local: Offset): Offset? =
        editorCoordinates?.takeIf { it.isAttached }?.localToRoot(local)

    /** The document position nearest to [root], anchored to the closest selectable field. */
    fun docPositionAt(state: RichDocumentState, root: Offset): RichDocPosition? {
        val index = fieldBounds.entries
            .filter { state.blockTextLength(it.key) != null }
            .minByOrNull { (_, bounds) ->
                when {
                    root.y < bounds.top -> bounds.top - root.y
                    root.y > bounds.bottom -> root.y - bounds.bottom
                    else -> 0f
                }
            }?.key ?: return null
        val bounds = fieldBounds[index] ?: return null
        val layout = textLayoutAt(index) ?: return RichDocPosition(index, 0)
        val offset = layout.getOffsetForPosition(root - bounds.topLeft)
        return RichDocPosition(index, offset.coerceIn(0, state.blockTextLength(index) ?: 0))
    }

    /** Root-coordinate anchor rect for the selection menu, at a visible selection edge. */
    fun selectionMenuRect(state: RichDocumentState): Rect? {
        val selection = state.documentSelection ?: return null
        return edgeCursorRect(selection.end) ?: edgeCursorRect(selection.start)
    }

    /** Root-coordinate cursor rect at [position], or null while its block is not composed. */
    fun edgeCursorRect(position: RichDocPosition): Rect? {
        val bounds = fieldBounds[position.blockIndex] ?: return null
        val layout = textLayoutAt(position.blockIndex) ?: return bounds
        val cursor = layout.getCursorRect(
            position.offset.coerceIn(0, layout.layoutInput.text.length),
        )
        return cursor.translate(bounds.topLeft)
    }
}

/** The directly editable field state of the block at [index], or null for raw/divider rows. */
internal fun editableFieldStateAt(state: RichDocumentState, index: Int): TextFieldState? =
    when (val block = state.blocks.getOrNull(index)) {
        is RichTextBlockEditState -> block.text.textFieldState
        is CodeBlockEditState -> block.code
        else -> null
    }

/**
 * Cross-block text selection, layered over the native per-field behavior instead of replacing
 * it: a long-press and drag INSIDE one block is handled entirely by that block's text field
 * (word snap, magnifier, handles — all native). This detector watches the gesture passively on
 * the tunneling pass and takes over only when the drag leaves the origin block — at that point
 * it steals the pointer, converts the field's in-progress selection into a document-level
 * [RichDocSelection] anchor, and extends it across blocks while the finger moves. A word
 * selection that ends WITHOUT leaving its block is handed to [onConvertSelection] on release,
 * so the handles the user then grabs are the cross-block ones.
 *
 * Raw-source rows keep their own [SelectionContainer]; a gesture starting on one is ignored.
 */
internal suspend fun PointerInputScope.detectDocumentSelection(
    state: RichDocumentState,
    geometry: RichSelectionGeometry,
    haptics: HapticFeedback,
    onClearSelection: () -> Unit,
    onSelectionFinished: () -> Unit,
    onConvertSelection: (blockIndex: Int, range: TextRange) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (state.documentSelection != null) onClearSelection()

        val downRoot = geometry.rootPositionOf(down.position) ?: return@awaitEachGesture
        if (geometry.rawBounds.values.any { it.contains(downRoot) }) return@awaitEachGesture
        val originIndex = geometry.fieldBounds.entries
            .firstOrNull { (index, bounds) ->
                bounds.contains(downRoot) && state.blockTextLength(index) != null
            }?.key ?: return@awaitEachGesture
        val originBounds = geometry.fieldBounds[originIndex] ?: return@awaitEachGesture
        val longPressDeadline = down.uptimeMillis + viewConfiguration.longPressTimeoutMillis

        // Passive phase: the origin field owns the gesture. Leave once it can't become a
        // cross-block drag: a lift, or the scroll claiming the gesture before the long-press
        // fired. Like the platform's own long-press detection, finger movement alone is NOT a
        // reason to leave — a held finger wobbles past the touch slop — only consumption is;
        // it lands during the main pass, so it shows on the PREVIOUS change when the next
        // event arrives on this initial pass.
        // The down itself is consumed by the field's own press handling, so it says nothing
        // about who owns the gesture; only a consumed MOVE means the scroll claimed it.
        var previousMove: PointerInputChange? = null
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
                ?: return@awaitEachGesture
            if (!change.pressed) {
                val fieldSelection = editableFieldStateAt(state, originIndex)?.selection
                if (fieldSelection != null && !fieldSelection.collapsed) {
                    onConvertSelection(originIndex, fieldSelection)
                }
                return@awaitEachGesture
            }
            // A scroll that claimed the gesture in the last frame before the deadline shows
            // its consumption only on the following event, which may already be past the
            // deadline — so the previous-move check applies on both sides of it.
            if (previousMove?.isConsumed == true) return@awaitEachGesture
            if (change.uptimeMillis < longPressDeadline) {
                previousMove = change
                continue
            }
            val root = geometry.rootPositionOf(change.position) ?: continue
            val bounds = geometry.fieldBounds[originIndex] ?: originBounds
            if (root.y < bounds.top || root.y > bounds.bottom) break
        }

        // Takeover: seed the anchor from whatever the origin field selected natively, collapse
        // the field selection so its handles retire, then own the pointer until lift.
        val originFieldState = editableFieldStateAt(state, originIndex)
        val fieldSelection = originFieldState?.selection ?: TextRange.Zero
        val anchorStart = RichDocPosition(originIndex, fieldSelection.min)
        val anchorEnd = RichDocPosition(originIndex, fieldSelection.max)
        state.documentSelection = RichDocSelection(anchorStart, anchorEnd)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        // The origin field's own selection is collapsed for the whole takeover, not just once:
        // its in-flight drag still processes the boundary-crossing event on the main pass
        // (after this initial-pass takeover, too late to consume) and re-asserts the native
        // word selection, which would otherwise linger beneath the document selection.
        fun retireOriginSelection() {
            if (originFieldState != null && !originFieldState.selection.collapsed) {
                originFieldState.edit { selection = TextRange(originFieldState.selection.max) }
            }
        }
        retireOriginSelection()

        fun extendTo(position: RichDocPosition) {
            state.documentSelection = when {
                position < anchorStart -> RichDocSelection(position, anchorEnd)
                position > anchorEnd -> RichDocSelection(anchorStart, position)
                else -> RichDocSelection(anchorStart, anchorEnd)
            }
        }

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.fastForEach { it.consume() }
            retireOriginSelection()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            val root = geometry.rootPositionOf(change.position)
            if (root != null) {
                geometry.docPositionAt(state, root)?.let(::extendTo)
                geometry.autoscroll = geometry.editorCoordinates?.takeIf { it.isAttached }
                    ?.let { coordinates ->
                        selectionAutoscrollVelocity(coordinates.boundsInRoot(), root.y)
                            .takeIf { it != 0f }
                            ?.let { SelectionAutoscroll(it, root, ::extendTo) }
                    }
            }
            if (!change.pressed) break
        }
        geometry.autoscroll = null
        retireOriginSelection()
        if (state.documentSelection != null) onSelectionFinished()
    }
}

/**
 * Native floating text toolbar for an active document selection: Copy and Cut put the
 * selection on the clipboard as Markdown text — readable anywhere, and pasting it back into
 * the rich editor re-applies the structure. Paste replaces the selection with the clipboard
 * content (parsed as Markdown); Select All extends the selection to the whole document.
 */
internal fun showDocumentSelectionMenu(
    state: RichDocumentState,
    geometry: RichSelectionGeometry,
    textToolbar: TextToolbar,
    clipboard: Clipboard,
    scope: CoroutineScope,
) {
    val rect = geometry.selectionMenuRect(state) ?: return
    fun copyToClipboard() {
        state.selectedDocument()?.let { document ->
            val markdown = markdownSerializer.convert(document)
            scope.launch {
                clipboard.setClipEntry(
                    ClipData.newPlainText(null, markdown).toClipEntry(),
                )
            }
        }
    }
    textToolbar.showMenu(
        rect = rect,
        onCopyRequested = {
            copyToClipboard()
            state.documentSelection = null
            textToolbar.hide()
        },
        onCutRequested = {
            copyToClipboard()
            state.deleteDocumentSelection()
            textToolbar.hide()
        },
        onPasteRequested = {
            textToolbar.hide()
            scope.launch {
                val text = clipboard.getClipEntry()?.clipData
                    ?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)?.text?.toString()
                if (!text.isNullOrEmpty()) state.replaceDocumentSelection(text)
            }
        },
        onSelectAllRequested = {
            state.selectAllBlocks()
            showDocumentSelectionMenu(state, geometry, textToolbar, clipboard, scope)
        },
    )
}

private val markdownSerializer by lazy { RichDocumentToMarkdownConverter() }

private val AutoscrollThreshold = 64.dp
private val AutoscrollMaxStep = 12.dp

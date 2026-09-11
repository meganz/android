package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange

/**
 * Structural identity of a text-bearing block in the rich editor; drives its chrome and text
 * style. Mutable per block (the toolbar retypes blocks in place, e.g. paragraph -> heading).
 */
sealed interface RichBlockKind {
    data object Paragraph : RichBlockKind
    data class Heading(val level: Int) : RichBlockKind

    /** [startNumber] only on the first item of an ordered run not starting at 1. */
    data class Item(
        val ordered: Boolean,
        val indent: Int,
        val checked: Boolean?,
        val startNumber: Int? = null,
    ) : RichBlockKind

    data class Quote(val depth: Int) : RichBlockKind
}

/** Editing state of one block in the rich editor. */
sealed interface RichBlockEditState

/** A text-bearing block: paragraph, heading, list item, or quote. Always directly editable. */
class RichTextBlockEditState(
    kind: RichBlockKind,
    val text: RichTextBlockState,
) : RichBlockEditState {
    var kind: RichBlockKind by mutableStateOf(kind)
}

/** A fenced/indented code block, edited as plain monospace text inside its box. */
class CodeBlockEditState(
    val language: String?,
    initialCode: String,
) : RichBlockEditState {
    val code = TextFieldState(initialCode)
}

class ThematicBreakEditState : RichBlockEditState

/** Unsupported Markdown kept verbatim; rendered read-only and re-emitted byte-identical. */
class RawSourceEditState(val source: String) : RichBlockEditState

/**
 * A GFM table whose cell TEXTS are editable in place (structure is not). [originalSource]
 * holds the verbatim Markdown so an untouched table serializes byte-identically; it is null
 * once the table came from an edited snapshot and normalized emission is already the truth.
 */
class TableBlockEditState(
    val originalSource: String?,
    val initial: RichBlock.Table,
) : RichBlockEditState {
    val alignments: List<RichTableAlignment?> = initial.alignments
    val header: List<RichTextBlockState> =
        initial.header.map { RichTextBlockState(it.text, it.spans) }
    val rows: List<List<RichTextBlockState>> =
        initial.rows.map { row -> row.map { RichTextBlockState(it.text, it.spans) } }

    val allCells: List<RichTextBlockState> = header + rows.flatten()

    fun toTable(): RichBlock.Table = RichBlock.Table(
        alignments = alignments,
        header = header.map { it.toRichText() },
        rows = rows.map { row -> row.map { it.toRichText() } },
    )

    /** Untouched tables keep their verbatim bytes; edited ones serialize normalized. */
    fun toDocumentBlock(): RichBlock {
        val current = toTable()
        return if (originalSource != null && current == initial) {
            RichBlock.RawSource(originalSource)
        } else {
            current
        }
    }
}

/** A one-shot request to focus a block and place its selection, consumed by the editor UI. */
data class RichFocusRequest(val index: Int, val selection: TextRange)

/** A one-shot request to split a block (Enter), consumed by the editor UI on the UI thread. */
data class RichSplitRequest(val index: Int, val start: Int, val end: Int)

/** A one-shot request to re-apply a pasted multi-line insert as parsed Markdown. */
data class RichSmartInsertRequest(
    val index: Int,
    val start: Int,
    val end: Int,
    val text: String,
)

/** A position in the rich document: a block index plus a character offset in its text. */
data class RichDocPosition(val blockIndex: Int, val offset: Int) : Comparable<RichDocPosition> {
    override fun compareTo(other: RichDocPosition): Int =
        compareValuesBy(this, other, { it.blockIndex }, { it.offset })
}

/** A cross-block document selection; [start] <= [end] in document order. */
data class RichDocSelection(val start: RichDocPosition, val end: RichDocPosition) {
    companion object {
        /** Builds a selection with the two positions put in document order. */
        fun between(a: RichDocPosition, b: RichDocPosition): RichDocSelection =
            if (a <= b) RichDocSelection(a, b) else RichDocSelection(b, a)
    }
}

/**
 * Editing state of a whole rich document: the observable block list plus which block holds
 * focus. Bridges the immutable [RichDocument] model to always-editable per-block state, and
 * back via [toDocument] for serialization, dirty checks, and save.
 */
class RichDocumentState(document: RichDocument) {

    val blocks = mutableStateListOf<RichBlockEditState>().apply {
        document.blocks.forEach { add(it.toEditState()) }
    }

    var focusedIndex: Int? by mutableStateOf(null)

    var pendingFocus: RichFocusRequest? by mutableStateOf(null)

    var pendingSplit: RichSplitRequest? by mutableStateOf(null)

    /**
     * The cross-block "document selection" (long-press + drag across blocks). Independent of
     * the per-field editing selection: it is painted through the span pipeline and copied via
     * [selectedText]; the fields never see it.
     */
    var documentSelection: RichDocSelection? by mutableStateOf(null)

    /**
     * One-shot request to replace [documentSelection] with typed text ("" = plain delete),
     * recorded from the text input pipeline; the editor performs it on the UI thread.
     */
    var pendingSelectionReplacement: String? by mutableStateOf(null)

    fun requestDeleteSelection() {
        pendingSelectionReplacement = ""
    }

    fun requestReplaceSelection(text: String) {
        pendingSelectionReplacement = text
    }

    /** One-shot request for a paste-like insert, recorded from the text input pipeline. */
    var pendingSmartInsert: RichSmartInsertRequest? by mutableStateOf(null)

    fun requestSmartInsert(index: Int, start: Int, end: Int, text: String) {
        pendingSmartInsert = RichSmartInsertRequest(index, start, end, text)
    }

    private val markdownConverter by lazy { MarkdownToRichDocumentConverter() }

    // Document-level undo: per-field text undo cannot cover structural operations
    // (split/merge/retype) or span changes, and would desync spans from text, so the whole
    // document is checkpointed instead. Snapshots are the immutable model — cheap to hold
    // and compare for the single-chunk documents rich mode is gated to.
    private val undoStack = ArrayDeque<RichDocument>().apply { addLast(document) }
    private val redoStack = ArrayDeque<RichDocument>()

    /** The focused text-bearing block, or null (no focus, or a non-text block). */
    val focusedTextBlock: RichTextBlockEditState?
        get() = focusedIndex?.let { blocks.getOrNull(it) } as? RichTextBlockEditState

    /** The focused table cell's text state, while focus is inside a table. */
    var focusedCell: RichTextBlockState? by mutableStateOf(null)

    /**
     * The text state INLINE format actions target: a focused table cell, else the focused
     * block. Structural actions keep resolving [focusedTextBlock] and so no-op in cells.
     */
    val focusedInlineText: RichTextBlockState?
        get() = focusedCell ?: focusedTextBlock?.text

    /**
     * Checkpoints the current document for undo. Called before every mutating operation (the
     * structural operations checkpoint themselves) and after typing bursts settle. An
     * unchanged document is not stacked twice; any new checkpoint invalidates redo.
     */
    fun recordUndo() {
        val current = toDocument()
        if (undoStack.lastOrNull() == current) return
        undoStack.addLast(current)
        while (undoStack.size > UndoCapacity) undoStack.removeFirst()
        redoStack.clear()
    }

    /**
     * Restores the newest checkpoint older than the current document.
     *
     * Non-destructive by invariant: the restored checkpoint STAYS on the stack as the current
     * state's own checkpoint. That keeps a no-op undo from draining the baseline, and it makes
     * the debounced typing collector harmless after a restore — its [recordUndo] finds the
     * current document already on top and dedups instead of clearing the redo history.
     */
    fun undo(): Boolean {
        val current = toDocument()
        while (undoStack.size > 1 && undoStack.last() == current) {
            undoStack.removeLast()
        }
        val snapshot = undoStack.lastOrNull() ?: run {
            undoStack.addLast(current)
            return false
        }
        if (snapshot == current) return false
        redoStack.addLast(current)
        restore(snapshot)
        return true
    }

    fun redo(): Boolean {
        val snapshot = redoStack.removeLastOrNull() ?: return false
        val current = toDocument()
        // Capture drift typed since the undo (normally none: a checkpointed edit clears redo).
        if (undoStack.lastOrNull() != current) undoStack.addLast(current)
        undoStack.addLast(snapshot)
        while (undoStack.size > UndoCapacity) undoStack.removeFirst()
        restore(snapshot)
        return true
    }

    private fun restore(document: RichDocument) {
        blocks.clear()
        document.blocks.forEach { blocks.add(it.toEditState()) }
        focusedIndex = focusedIndex?.coerceIn(0, (blocks.size - 1).coerceAtLeast(0))
        pendingFocus = null
        pendingSplit = null
        pendingSelectionReplacement = null
        pendingSmartInsert = null
        documentSelection = null
        focusedCell = null
    }

    /** The part of [documentSelection] covering the block at [index], or null. */
    fun documentSelectionIn(index: Int): TextRange? {
        val selection = documentSelection ?: return null
        if (index < selection.start.blockIndex || index > selection.end.blockIndex) return null
        val length = blockTextLength(index) ?: return null
        val from = if (index == selection.start.blockIndex) selection.start.offset else 0
        val to = if (index == selection.end.blockIndex) selection.end.offset else length
        val range = TextRange(from.coerceIn(0, length), to.coerceIn(0, length))
        return range.takeIf { it.min < it.max || selection.start.blockIndex != selection.end.blockIndex }
    }

    /**
     * Moves one edge of [documentSelection] to [position] (a selection-handle drag). Edges
     * cannot cross, matching native handle behavior: a move that would pass the other edge is
     * ignored.
     */
    fun moveSelectionEdge(isStart: Boolean, position: RichDocPosition) {
        val selection = documentSelection ?: return
        documentSelection = when {
            isStart && position < selection.end -> selection.copy(start = position)
            !isStart && position > selection.start -> selection.copy(end = position)
            else -> selection
        }
    }

    /**
     * Deletes the content covered by [documentSelection]: partial text on the edge blocks,
     * whole blocks in between, and the two remnants joined when both are text blocks — one
     * undo step. Returns false when there is nothing to delete.
     */
    fun deleteDocumentSelection(): Boolean {
        val selection = documentSelection?.takeIf { it.start != it.end } ?: return false
        if (blocks.isEmpty()) return false
        documentSelection = null
        recordUndo()
        val startIndex = selection.start.blockIndex.coerceIn(0, blocks.size - 1)
        val endIndex = selection.end.blockIndex.coerceIn(startIndex, blocks.size - 1)
        if (startIndex == endIndex) {
            deleteWithinBlock(startIndex, selection.start.offset, selection.end.offset)
        } else {
            deleteWithinBlock(endIndex, 0, selection.end.offset)
            for (index in endIndex - 1 downTo startIndex + 1) {
                blocks.removeAt(index)
            }
            deleteWithinBlock(
                startIndex,
                selection.start.offset,
                blockTextLength(startIndex) ?: Int.MAX_VALUE,
            )
            val first = blocks.getOrNull(startIndex)
            val second = blocks.getOrNull(startIndex + 1)
            if (first is RichTextBlockEditState && second is RichTextBlockEditState) {
                val joinAt = first.text.textFieldState.text.length
                first.text.textFieldState.edit { append(second.text.textFieldState.text) }
                first.text.spans = RichSpanAdjuster.normalize(
                    first.text.spans + second.text.spans.map {
                        it.copy(start = it.start + joinAt, end = it.end + joinAt)
                    },
                )
                blocks.removeAt(startIndex + 1)
            }
        }
        val caret = selection.start.offset.coerceIn(0, blockTextLength(startIndex) ?: 0)
        pendingFocus = RichFocusRequest(startIndex, TextRange(caret))
        return true
    }

    /**
     * Replaces [documentSelection] with [insert]: the selection is deleted and the typed text
     * lands at the join; a lone newline instead splits the merged block there (Enter).
     */
    fun replaceDocumentSelection(insert: String): Boolean {
        if (!deleteDocumentSelection()) return false
        val target = pendingFocus ?: return true
        if (insert.isEmpty()) return true
        when (val block = blocks.getOrNull(target.index)) {
            // Code is the one multi-line block kind: newlines go in verbatim.
            is CodeBlockEditState -> {
                val at = target.selection.start
                block.code.edit { insert(at, insert) }
                pendingFocus = RichFocusRequest(target.index, TextRange(at + insert.length))
            }

            is RichTextBlockEditState ->
                insertContent(target.index, target.selection.start, insert)

            else -> Unit
        }
        return true
    }

    /**
     * A paste-like insert at [start]..[end] of the text block at [index]: Markdown syntax in
     * the pasted text is applied as real structure, plain text falls back to line-per-block
     * insertion. One undo step.
     */
    fun smartInsert(index: Int, start: Int, end: Int, text: String) {
        if (blocks.getOrNull(index) !is RichTextBlockEditState) return
        recordUndo()
        deleteWithinBlock(index, start, end)
        insertContent(index, start, text)
    }

    /**
     * Inserted text that parses into Markdown STRUCTURE (several blocks, a non-paragraph
     * kind, inline styles) is spliced in as blocks and spans — pasted Markdown formats
     * itself. Anything else inserts as plain text, one block per line.
     */
    private fun insertContent(index: Int, at: Int, text: String) {
        val parsed = markdownConverter.convert(text)
        val single = parsed.blocks.singleOrNull()
        val isPlain = parsed.blocks.isEmpty() ||
                (single is RichBlock.Paragraph && single.text.spans.isEmpty())
        if (isPlain) {
            insertPlainSegments(index, at, text)
        } else {
            spliceBlocks(index, at, parsed.blocks)
        }
    }

    /** Text blocks are single-line by invariant: each newline becomes a block split. */
    private fun insertPlainSegments(startIndex: Int, startAt: Int, text: String) {
        var index = startIndex
        var at = startAt
        text.split("\n").forEachIndexed { segmentIndex, segment ->
            if (segmentIndex > 0) {
                performSplit(index, at)
                index++
                at = 0
            }
            if (segment.isNotEmpty()) {
                insertIntoTextBlock(index, at, segment)
                at += segment.length
            }
        }
        pendingFocus = RichFocusRequest(index, TextRange(at))
    }

    private fun spliceBlocks(index: Int, at: Int, newBlocks: List<RichBlock>) {
        val target = blocks.getOrNull(index) as? RichTextBlockEditState ?: return
        val targetKind = target.kind
        val text = target.text.textFieldState.text.toString()
        val split = at.coerceIn(0, text.length)
        val tailText = text.substring(split)
        val (headSpans, tailSpans) = splitSpans(target.text.spans, split, split)
        target.text.textFieldState.edit { replace(split, length, "") }
        target.text.spans = headSpans

        val states = newBlocks.map { it.toEditState() }.toMutableList()
        val firstState = states.firstOrNull() ?: return
        // Pasting at the start of a block adopts the first pasted block wholesale, kind
        // included. Mid-text, only a plain PARAGRAPH merges into the sentence — a pasted
        // heading, list item or quote keeps its identity as its own block.
        if (split == 0) {
            blocks[index] = firstState
            states.removeAt(0)
        } else if (firstState is RichTextBlockEditState && firstState.kind == RichBlockKind.Paragraph) {
            target.text.textFieldState.edit { append(firstState.text.textFieldState.text) }
            target.text.spans = RichSpanAdjuster.normalize(
                target.text.spans + firstState.text.spans.map {
                    it.copy(start = it.start + split, end = it.end + split)
                },
            )
            states.removeAt(0)
        }
        states.forEachIndexed { offset, state -> blocks.add(index + 1 + offset, state) }
        val lastIndex = index + states.size

        // The remainder of the split block joins the end of the pasted content, keeping the
        // caret at the seam like a native paste.
        val lastBlock = blocks.getOrNull(lastIndex)
        val caret: RichFocusRequest = if (tailText.isEmpty() && tailSpans.isEmpty()) {
            RichFocusRequest(lastIndex, TextRange(blockTextLength(lastIndex) ?: 0))
        } else if (lastBlock is RichTextBlockEditState && lastBlock.kind == RichBlockKind.Paragraph) {
            val joinAt = lastBlock.text.textFieldState.text.length
            lastBlock.text.textFieldState.edit { append(tailText) }
            lastBlock.text.spans = RichSpanAdjuster.normalize(
                lastBlock.text.spans + tailSpans.map {
                    it.copy(start = it.start + joinAt, end = it.end + joinAt)
                },
            )
            RichFocusRequest(lastIndex, TextRange(joinAt))
        } else {
            val tailKind = when (targetKind) {
                is RichBlockKind.Item ->
                    targetKind.copy(checked = targetKind.checked?.let { false }, startNumber = null)

                else -> targetKind
            }
            blocks.add(
                lastIndex + 1,
                RichTextBlockEditState(tailKind, RichTextBlockState(tailText, tailSpans)),
            )
            RichFocusRequest(lastIndex + 1, TextRange.Zero)
        }
        pendingFocus = caret
    }

    private fun insertIntoTextBlock(index: Int, at: Int, text: String) {
        val block = blocks.getOrNull(index) as? RichTextBlockEditState ?: return
        block.text.textFieldState.edit { insert(at, text) }
        block.text.spans = RichSpanAdjuster.adjust(
            block.text.spans,
            listOf(RichSpanAdjuster.TextChange(at, at, at, at + text.length)),
        )
    }

    private fun deleteWithinBlock(index: Int, from: Int, to: Int) {
        when (val block = blocks.getOrNull(index)) {
            is RichTextBlockEditState -> {
                val length = block.text.textFieldState.text.length
                val start = from.coerceIn(0, length)
                val end = to.coerceIn(start, length)
                if (start == end) return
                block.text.textFieldState.edit { delete(start, end) }
                block.text.spans = RichSpanAdjuster.adjust(
                    block.text.spans,
                    listOf(RichSpanAdjuster.TextChange(start, end, start, start)),
                )
            }

            is CodeBlockEditState -> {
                val length = block.code.text.length
                val start = from.coerceIn(0, length)
                val end = to.coerceIn(start, length)
                if (start != end) block.code.edit { delete(start, end) }
            }

            else -> Unit
        }
    }

    /**
     * Toggles [style] over the active cross-block [documentSelection]: removed when every
     * selected character in every covered text block already carries it, applied otherwise.
     * One undo step; the selection survives so styles can be stacked. Returns false when
     * there is no document selection.
     */
    fun toggleStyleOverDocumentSelection(style: RichSpanStyle): Boolean {
        if (documentSelection == null) return false
        val ranges = documentSelectionRanges()
        if (ranges.isEmpty()) return true
        recordUndo()
        val covered = ranges.all { (text, range) ->
            RichSpanAdjuster.isCovered(text.spans, range.min, range.max, style)
        }
        ranges.forEach { (text, range) ->
            text.spans = if (covered) {
                RichSpanAdjuster.toggle(text.spans, range.min, range.max, style)
            } else {
                RichSpanAdjuster.normalize(
                    text.spans + RichSpan(range.min, range.max, style),
                )
            }
        }
        return true
    }

    /** True when [style] covers the whole [documentSelection]; null without one. */
    fun documentSelectionStyleCovered(style: RichSpanStyle): Boolean? =
        documentSelectionCoveredStyles(setOf(style))?.contains(style)

    /**
     * The subset of [styles] covering every selected character of [documentSelection] (null
     * without one) — one range walk for all styles, cheap enough for per-frame active states.
     */
    fun documentSelectionCoveredStyles(styles: Set<RichSpanStyle>): Set<RichSpanStyle>? {
        if (documentSelection == null) return null
        val ranges = documentSelectionRanges()
        if (ranges.isEmpty()) return emptySet()
        return styles.filterTo(mutableSetOf()) { style ->
            ranges.all { (text, range) ->
                RichSpanAdjuster.isCovered(text.spans, range.min, range.max, style)
            }
        }
    }

    private fun documentSelectionRanges(): List<Pair<RichTextBlockState, TextRange>> {
        val selection = documentSelection ?: return emptyList()
        val ranges = mutableListOf<Pair<RichTextBlockState, TextRange>>()
        for (index in selection.start.blockIndex..selection.end.blockIndex) {
            val text = (blocks.getOrNull(index) as? RichTextBlockEditState)?.text ?: continue
            val range = documentSelectionIn(index) ?: continue
            if (range.min < range.max) ranges += text to range
        }
        return ranges
    }

    /** Selects every selectable block, first character to last. */
    fun selectAllBlocks() {
        val first = blocks.indices.firstOrNull { blockTextLength(it) != null } ?: return
        val last = blocks.indices.last { blockTextLength(it) != null }
        documentSelection = RichDocSelection(
            RichDocPosition(first, 0),
            RichDocPosition(last, blockTextLength(last) ?: 0),
        )
    }

    /**
     * The selected content as plain text: partial text on the edge blocks, whole blocks in
     * between (raw source verbatim), joined with newlines. Null when nothing is selected.
     */
    fun selectedText(): String? {
        val selection = documentSelection?.takeIf { it.start != it.end } ?: return null
        val parts = mutableListOf<String>()
        for (index in selection.start.blockIndex..selection.end.blockIndex) {
            val text = when (val block = blocks.getOrNull(index)) {
                is RichTextBlockEditState -> block.text.textFieldState.text.toString()
                is CodeBlockEditState -> block.code.text.toString()
                is RawSourceEditState -> block.source
                is TableBlockEditState -> when (val document = block.toDocumentBlock()) {
                    is RichBlock.RawSource -> document.source
                    else -> richTableSerializer.convert(RichDocument(listOf(document)))
                }

                else -> null
            } ?: continue
            val range = when (blocks.getOrNull(index)) {
                is RawSourceEditState, is TableBlockEditState -> TextRange(0, text.length)
                else -> documentSelectionIn(index) ?: TextRange(0, text.length)
            }
            parts += text.substring(range.min, range.max)
        }
        return parts.joinToString("\n").takeIf { it.isNotEmpty() }
    }

    /**
     * The selected content as a standalone [RichDocument] for Markdown serialization: partial
     * edge blocks keep their kind and sliced spans, interior blocks are included whole. Null
     * when nothing is selected.
     */
    fun selectedDocument(): RichDocument? {
        val selection = documentSelection?.takeIf { it.start != it.end } ?: return null
        val sliced = mutableListOf<RichBlock>()
        for (index in selection.start.blockIndex..selection.end.blockIndex) {
            val from = if (index == selection.start.blockIndex) selection.start.offset else 0
            val to = if (index == selection.end.blockIndex) selection.end.offset else Int.MAX_VALUE
            slicedBlockAt(index, from, to)?.let(sliced::add)
        }
        return RichDocument(sliced).takeIf { it.blocks.isNotEmpty() }
    }

    private fun slicedBlockAt(index: Int, from: Int, to: Int): RichBlock? =
        when (val block = blocks.getOrNull(index)) {
            is RichTextBlockEditState -> {
                val text = block.text.textFieldState.text.toString()
                val start = from.coerceIn(0, text.length)
                val end = to.coerceIn(start, text.length)
                val upToEnd = splitSpans(block.text.spans, end, end).first
                val spans = splitSpans(upToEnd, start, start).second
                val richText = RichText(text.substring(start, end), spans)
                when (val kind = block.kind) {
                    RichBlockKind.Paragraph -> RichBlock.Paragraph(richText)
                    is RichBlockKind.Heading -> RichBlock.Heading(kind.level, richText)
                    is RichBlockKind.Item -> RichBlock.ListItem(
                        ordered = kind.ordered,
                        indent = kind.indent,
                        checked = kind.checked,
                        text = richText,
                        startNumber = kind.startNumber,
                    )

                    is RichBlockKind.Quote -> RichBlock.Quote(kind.depth, richText)
                }
            }

            is CodeBlockEditState -> {
                val text = block.code.text.toString()
                val start = from.coerceIn(0, text.length)
                val end = to.coerceIn(start, text.length)
                RichBlock.CodeBlock(block.language, text.substring(start, end))
            }

            is RawSourceEditState -> RichBlock.RawSource(block.source)
            is TableBlockEditState -> block.toDocumentBlock()
            is ThematicBreakEditState -> RichBlock.ThematicBreak
            else -> null
        }

    /** Length of the block's selectable text, or null when the block cannot anchor a selection. */
    fun blockTextLength(index: Int): Int? = when (val block = blocks.getOrNull(index)) {
        is RichTextBlockEditState -> block.text.textFieldState.text.length
        is CodeBlockEditState -> block.code.text.length
        else -> null
    }

    /** Checkbox tap on a task item; checkpointed for undo. */
    fun setTaskChecked(index: Int, checked: Boolean) {
        val block = blocks.getOrNull(index) as? RichTextBlockEditState ?: return
        val kind = block.kind as? RichBlockKind.Item ?: return
        recordUndo()
        block.kind = kind.copy(checked = checked)
    }

    /**
     * Records a split request from the field's input transformation. Only a snapshot-state
     * write (thread-safe): the input pipeline gives no main-thread guarantee and the field's
     * edit session is still open, so the actual [splitBlock] runs when the editor consumes
     * [pendingSplit] on the UI thread, after the session has committed.
     */
    fun requestSplit(index: Int, start: Int, end: Int = start) {
        pendingSplit = RichSplitRequest(index, start, end)
    }

    /**
     * Enter semantics: splits the text block at [index] into two, deleting [start]..[end]
     * (the selection Enter replaced) and moving the trailing text and spans into a new block.
     * List items and quotes continue (a completed task item continues unchecked); Enter at the
     * end of a heading starts a paragraph; Enter on an EMPTY item or quote steps out one level
     * instead of adding another marker.
     *
     * Must run on the UI thread outside any edit session of the affected block — Enter arrives
     * through [requestSplit]/[pendingSplit] instead of calling this directly.
     */
    fun splitBlock(index: Int, start: Int, end: Int = start) {
        recordUndo()
        performSplit(index, start, end)
    }

    private fun performSplit(index: Int, start: Int, end: Int = start) {
        val block = blocks.getOrNull(index) as? RichTextBlockEditState ?: return
        val kind = block.kind
        val text = block.text.textFieldState.text.toString()
        if (text.isEmpty()) {
            val demoted = demote(kind)
            if (demoted != null) {
                block.kind = demoted
                return
            }
        }
        val splitAt = start.coerceIn(0, text.length)
        val removeTo = end.coerceIn(splitAt, text.length)
        val afterText = text.substring(removeTo)
        val (beforeSpans, afterSpans) = splitSpans(block.text.spans, splitAt, removeTo)
        val newKind = when {
            kind is RichBlockKind.Heading && afterText.isEmpty() -> RichBlockKind.Paragraph
            // The continuation is never a run start, so it must not carry the start number.
            kind is RichBlockKind.Item ->
                kind.copy(checked = kind.checked?.let { false }, startNumber = null)

            else -> kind
        }
        block.text.textFieldState.edit { replace(splitAt, length, "") }
        block.text.spans = beforeSpans
        blocks.add(
            index + 1,
            RichTextBlockEditState(newKind, RichTextBlockState(afterText, afterSpans)),
        )
        pendingFocus = RichFocusRequest(index + 1, TextRange.Zero)
    }

    /**
     * Backspace-at-start semantics for the text block at [index]: a list item or quote first
     * sheds one structure level (indent/depth, then the marker itself); a plain block merges
     * into the preceding text block with the caret at the join, deletes a preceding divider,
     * and does nothing against a code/raw block or at the document start.
     *
     * @return true when the key press was handled and the default deletion must not run.
     */
    fun mergeBlockBackward(index: Int): Boolean {
        val block = blocks.getOrNull(index) as? RichTextBlockEditState ?: return false
        recordUndo()
        demote(block.kind)?.let {
            block.kind = it
            return true
        }
        return when (val previous = blocks.getOrNull(index - 1)) {
            is RichTextBlockEditState -> {
                val joinAt = previous.text.textFieldState.text.length
                previous.text.textFieldState.edit {
                    append(block.text.textFieldState.text)
                }
                previous.text.spans = RichSpanAdjuster.normalize(
                    previous.text.spans + block.text.spans.map {
                        it.copy(start = it.start + joinAt, end = it.end + joinAt)
                    },
                )
                blocks.removeAt(index)
                pendingFocus = RichFocusRequest(index - 1, TextRange(joinAt))
                true
            }

            is ThematicBreakEditState -> {
                blocks.removeAt(index - 1)
                pendingFocus = RichFocusRequest(index - 1, TextRange.Zero)
                true
            }

            else -> false
        }
    }

    /**
     * A kind change can recreate the field node (text style changes re-key it); this puts
     * focus and the caret back where they were.
     */
    private fun restoreFocusAfterRetype() {
        val index = focusedIndex ?: return
        val block = focusedTextBlock ?: return
        pendingFocus = RichFocusRequest(index, block.text.textFieldState.selection)
    }

    /** Toolbar heading action on the focused block: body -> H1 -> H2 -> H3 -> body. */
    fun cycleFocusedHeading() {
        val block = focusedTextBlock ?: return
        recordUndo()
        block.kind = when (val kind = block.kind) {
            is RichBlockKind.Heading ->
                if (kind.level < MaxCycledHeadingLevel) {
                    RichBlockKind.Heading(kind.level + 1)
                } else {
                    RichBlockKind.Paragraph
                }

            else -> RichBlockKind.Heading(1)
        }
        restoreFocusAfterRetype()
    }

    /**
     * Toolbar list action: toggles the focused block's list marker, flips its list type, or
     * sheds a task box back to a plain marker. Bullet, numbered, and task lists behave as
     * three mutually exclusive list flavors.
     */
    fun toggleFocusedListItem(ordered: Boolean) {
        val block = focusedTextBlock ?: return
        recordUndo()
        val kind = block.kind
        block.kind = when {
            kind is RichBlockKind.Item && kind.ordered == ordered && kind.checked == null ->
                RichBlockKind.Paragraph

            kind is RichBlockKind.Item -> kind.copy(ordered = ordered, checked = null)
            else -> RichBlockKind.Item(ordered = ordered, indent = 0, checked = null)
        }
        restoreFocusAfterRetype()
    }

    /** Toolbar checklist action: toggles the focused block's task box (unchecked when added). */
    fun toggleFocusedTaskItem() {
        val block = focusedTextBlock ?: return
        recordUndo()
        val kind = block.kind
        block.kind = when {
            kind is RichBlockKind.Item && kind.checked != null -> RichBlockKind.Paragraph
            kind is RichBlockKind.Item -> kind.copy(checked = false)
            else -> RichBlockKind.Item(ordered = false, indent = 0, checked = false)
        }
        restoreFocusAfterRetype()
    }

    /** Toolbar quote action: toggles the focused block between quote and paragraph. */
    fun toggleFocusedQuote() {
        val block = focusedTextBlock ?: return
        recordUndo()
        block.kind = when (block.kind) {
            is RichBlockKind.Quote -> RichBlockKind.Paragraph
            else -> RichBlockKind.Quote(1)
        }
        restoreFocusAfterRetype()
    }

    /** One structure level less, or null when [kind] has no structure to shed. */
    private fun demote(kind: RichBlockKind): RichBlockKind? = when (kind) {
        is RichBlockKind.Item ->
            if (kind.indent > 0) kind.copy(indent = kind.indent - 1) else RichBlockKind.Paragraph

        is RichBlockKind.Quote ->
            if (kind.depth > 1) kind.copy(depth = kind.depth - 1) else RichBlockKind.Paragraph

        RichBlockKind.Paragraph, is RichBlockKind.Heading -> null
    }

    private fun splitSpans(
        spans: List<RichSpan>,
        splitAt: Int,
        removeTo: Int,
    ): Pair<List<RichSpan>, List<RichSpan>> {
        val before = spans.mapNotNull { span ->
            if (span.start < splitAt) span.copy(end = minOf(span.end, splitAt)) else null
        }
        val after = spans.mapNotNull { span ->
            val start = maxOf(span.start, removeTo) - removeTo
            val end = span.end - removeTo
            if (end > start) RichSpan(start, end, span.style) else null
        }
        return RichSpanAdjuster.normalize(before) to RichSpanAdjuster.normalize(after)
    }

    fun toDocument(): RichDocument = RichDocument(
        blocks.map { block ->
            when (block) {
                is RichTextBlockEditState -> when (val kind = block.kind) {
                    RichBlockKind.Paragraph -> RichBlock.Paragraph(block.text.toRichText())
                    is RichBlockKind.Heading ->
                        RichBlock.Heading(kind.level, block.text.toRichText())

                    is RichBlockKind.Item -> RichBlock.ListItem(
                        ordered = kind.ordered,
                        indent = kind.indent,
                        checked = kind.checked,
                        text = block.text.toRichText(),
                        startNumber = kind.startNumber,
                    )

                    is RichBlockKind.Quote ->
                        RichBlock.Quote(kind.depth, block.text.toRichText())
                }

                is CodeBlockEditState ->
                    RichBlock.CodeBlock(block.language, block.code.text.toString())

                is ThematicBreakEditState -> RichBlock.ThematicBreak
                is RawSourceEditState -> RichBlock.RawSource(block.source)

                is TableBlockEditState -> block.toDocumentBlock()
            }
        },
    )

    private companion object {
        /** The heading toolbar cycles H1..H3; deeper levels are kept but not produced. */
        const val MaxCycledHeadingLevel = 3

        /** Undo checkpoints kept; bounds memory for the largest rich-capable documents. */
        const val UndoCapacity = 50
    }
}

private fun RichBlock.toEditState(): RichBlockEditState = when (this) {
    is RichBlock.Paragraph ->
        RichTextBlockEditState(RichBlockKind.Paragraph, text.toBlockState())

    is RichBlock.Heading ->
        RichTextBlockEditState(RichBlockKind.Heading(level), text.toBlockState())

    is RichBlock.ListItem -> RichTextBlockEditState(
        RichBlockKind.Item(ordered, indent, checked, startNumber),
        text.toBlockState(),
    )

    is RichBlock.Quote ->
        RichTextBlockEditState(RichBlockKind.Quote(depth), text.toBlockState())

    is RichBlock.CodeBlock -> CodeBlockEditState(language, code)
    is RichBlock.ThematicBreak -> ThematicBreakEditState()

    is RichBlock.RawSource ->
        richTableParser.parseTable(source)
            ?.let { TableBlockEditState(originalSource = source, initial = it) }
            ?: RawSourceEditState(source)

    is RichBlock.Table -> TableBlockEditState(originalSource = null, initial = this)
}

private fun RichText.toBlockState(): RichTextBlockState = RichTextBlockState(text, spans)

private val richTableParser by lazy { MarkdownToRichDocumentConverter() }

private val richTableSerializer by lazy { RichDocumentToMarkdownConverter() }

package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.foundation.text.input.TextFieldState
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
    data class Item(val ordered: Boolean, val indent: Int, val checked: Boolean?) : RichBlockKind
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

/** A one-shot request to focus a block and place its selection, consumed by the editor UI. */
data class RichFocusRequest(val index: Int, val selection: TextRange)

/** A one-shot request to split a block (Enter), consumed by the editor UI on the UI thread. */
data class RichSplitRequest(val index: Int, val start: Int, val end: Int)

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

    /** The focused text-bearing block, or null (no focus, or a non-text block). */
    val focusedTextBlock: RichTextBlockEditState?
        get() = focusedIndex?.let { blocks.getOrNull(it) } as? RichTextBlockEditState

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
            kind is RichBlockKind.Item -> kind.copy(checked = kind.checked?.let { false })
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

    /** Toolbar heading action on the focused block: body -> H1 -> H2 -> H3 -> body. */
    fun cycleFocusedHeading() {
        val block = focusedTextBlock ?: return
        block.kind = when (val kind = block.kind) {
            is RichBlockKind.Heading ->
                if (kind.level < MaxCycledHeadingLevel) {
                    RichBlockKind.Heading(kind.level + 1)
                } else {
                    RichBlockKind.Paragraph
                }

            else -> RichBlockKind.Heading(1)
        }
    }

    /** Toolbar list action: toggles the focused block's list marker, or flips its list type. */
    fun toggleFocusedListItem(ordered: Boolean) {
        val block = focusedTextBlock ?: return
        val kind = block.kind
        block.kind = when {
            kind is RichBlockKind.Item && kind.ordered == ordered -> RichBlockKind.Paragraph
            kind is RichBlockKind.Item -> kind.copy(ordered = ordered)
            else -> RichBlockKind.Item(ordered = ordered, indent = 0, checked = null)
        }
    }

    /** Toolbar quote action: toggles the focused block between quote and paragraph. */
    fun toggleFocusedQuote() {
        val block = focusedTextBlock ?: return
        block.kind = when (block.kind) {
            is RichBlockKind.Quote -> RichBlockKind.Paragraph
            else -> RichBlockKind.Quote(1)
        }
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
                    )

                    is RichBlockKind.Quote ->
                        RichBlock.Quote(kind.depth, block.text.toRichText())
                }

                is CodeBlockEditState ->
                    RichBlock.CodeBlock(block.language, block.code.text.toString())

                is ThematicBreakEditState -> RichBlock.ThematicBreak
                is RawSourceEditState -> RichBlock.RawSource(block.source)
            }
        },
    )

    private companion object {
        /** The heading toolbar cycles H1..H3; deeper levels are kept but not produced. */
        const val MaxCycledHeadingLevel = 3
    }
}

private fun RichBlock.toEditState(): RichBlockEditState = when (this) {
    is RichBlock.Paragraph ->
        RichTextBlockEditState(RichBlockKind.Paragraph, text.toBlockState())

    is RichBlock.Heading ->
        RichTextBlockEditState(RichBlockKind.Heading(level), text.toBlockState())

    is RichBlock.ListItem -> RichTextBlockEditState(
        RichBlockKind.Item(ordered, indent, checked),
        text.toBlockState(),
    )

    is RichBlock.Quote ->
        RichTextBlockEditState(RichBlockKind.Quote(depth), text.toBlockState())

    is RichBlock.CodeBlock -> CodeBlockEditState(language, code)
    is RichBlock.ThematicBreak -> ThematicBreakEditState()
    is RichBlock.RawSource -> RawSourceEditState(source)
}

private fun RichText.toBlockState(): RichTextBlockState = RichTextBlockState(text, spans)

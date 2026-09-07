package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import mega.privacy.android.feature.texteditor.components.MarkdownInlineStyles
import mega.privacy.android.feature.texteditor.components.rememberMarkdownColors

/**
 * Keeps a block's [RichTextBlockState.spans] attached to the right characters: on every user
 * edit the change list is fed through [RichSpanAdjuster], applying the active typing styles to
 * inserted text. Programmatic edits (`TextFieldState.edit`) bypass this by design — callers
 * mutating text directly must adjust spans themselves.
 *
 * A lone newline never reaches the text: blocks are single units, so Enter is reverted and
 * reported through [onSplit] with the replaced range. [onSplit] runs inside the text input
 * pipeline — no main-thread guarantee, and the field's edit session is still open — so it must
 * only RECORD the request (a snapshot-state write, e.g. [RichDocumentState.requestSplit]); the
 * actual document mutation happens when the editor consumes the request on the UI thread.
 */
@OptIn(ExperimentalFoundationApi::class)
class RichSpanInputTransformation(
    private val state: RichTextBlockState,
    private val onSplit: ((start: Int, end: Int) -> Unit)? = null,
    private val documentSelectionActive: (() -> Boolean)? = null,
    private val onReplaceDocumentSelection: ((String) -> Unit)? = null,
    private val onSmartInsert: ((start: Int, end: Int, text: String) -> Unit)? = null,
    private val sanitizeNewlines: Boolean = false,
) : InputTransformation {

    override fun TextFieldBuffer.transformInput() {
        if (changes.changeCount == 0) return
        if (interceptDocumentSelectionEdit(documentSelectionActive, onReplaceDocumentSelection)) {
            return
        }
        if (changes.changeCount == 1) {
            val newRange = changes.getRange(0)
            val inserted = asCharSequence().subSequence(newRange.min, newRange.max).toString()
            if (onSplit != null && inserted == "\n") {
                val originalRange = changes.getOriginalRange(0)
                revertAllChanges()
                onSplit.invoke(originalRange.min, originalRange.max)
                return
            }
            // A multi-line insert is a paste, never typing (IME word commits carry no
            // newline): re-applied on the UI thread as parsed Markdown, so pasted syntax
            // formats itself.
            if (onSmartInsert != null && inserted.length > 1 && inserted.contains('\n')) {
                val originalRange = changes.getOriginalRange(0)
                revertAllChanges()
                onSmartInsert.invoke(originalRange.min, originalRange.max, inserted)
                return
            }
            // Single-line fields (table cells): a pasted newline becomes a space, then the
            // rewritten change list flows into the span adjustment below as usual.
            if (sanitizeNewlines && inserted.length > 1 && inserted.contains('\n')) {
                val originalRange = changes.getOriginalRange(0)
                revertAllChanges()
                replace(originalRange.min, originalRange.max, inserted.replace('\n', ' '))
            }
        }
        val edits = (0 until changes.changeCount).map { index ->
            val newRange = changes.getRange(index)
            val originalRange = changes.getOriginalRange(index)
            RichSpanAdjuster.TextChange(
                originalStart = originalRange.min,
                originalEnd = originalRange.max,
                newStart = newRange.min,
                newEnd = newRange.max,
            )
        }
        state.spans = RichSpanAdjuster.adjust(state.spans, edits, state.typingStyles)
        state.editCount++
    }
}

/**
 * Applies a block's spans as visual styles; the text itself is never touched. When
 * [documentSelection] reports a range, it is painted as a selection highlight on top — the
 * cross-block selection is display-only state the field itself never sees.
 */
class RichSpanOutputTransformation(
    private val state: RichTextBlockState,
    private val styles: RichSpanVisualStyles,
    private val documentSelection: (() -> TextRange?)? = null,
) : OutputTransformation {

    override fun TextFieldBuffer.transformOutput() {
        state.spans.forEach { span ->
            if (span.end <= length) {
                addStyle(styles.forStyle(span.style), span.start, span.end)
            }
        }
        addDocumentSelectionHighlight(styles, documentSelection)
    }
}

/**
 * An edit typed while a cross-block document selection is active acts on THAT selection: the
 * field edit is reverted and re-recorded as a replace-selection request — backspace deletes
 * the selection, typed text replaces it (the actual document mutation happens on the UI
 * thread, like splits). Returns true when intercepted.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun TextFieldBuffer.interceptDocumentSelectionEdit(
    documentSelectionActive: (() -> Boolean)?,
    onReplaceDocumentSelection: ((String) -> Unit)?,
): Boolean {
    if (onReplaceDocumentSelection == null || documentSelectionActive?.invoke() != true) {
        return false
    }
    val inserted = buildString {
        (0 until changes.changeCount).forEach { index ->
            val range = changes.getRange(index)
            append(asCharSequence().subSequence(range.min, range.max))
        }
    }
    revertAllChanges()
    onReplaceDocumentSelection.invoke(inserted)
    return true
}

/**
 * Edit handling for fields without a span engine (code blocks) while a cross-block selection
 * is active: backspace and typed text are redirected to the document selection.
 */
@OptIn(ExperimentalFoundationApi::class)
class DocumentSelectionInputTransformation(
    private val documentSelectionActive: () -> Boolean,
    private val onReplaceDocumentSelection: (String) -> Unit,
) : InputTransformation {

    override fun TextFieldBuffer.transformInput() {
        if (changes.changeCount == 0) return
        interceptDocumentSelectionEdit(documentSelectionActive, onReplaceDocumentSelection)
    }
}

/**
 * Selection-highlight-only output transformation for fields without a span engine (code
 * blocks), so the cross-block selection paints over them too.
 */
class DocumentSelectionOutputTransformation(
    private val styles: RichSpanVisualStyles,
    private val documentSelection: () -> TextRange?,
) : OutputTransformation {

    override fun TextFieldBuffer.transformOutput() {
        addDocumentSelectionHighlight(styles, documentSelection)
    }
}

private fun TextFieldBuffer.addDocumentSelectionHighlight(
    styles: RichSpanVisualStyles,
    documentSelection: (() -> TextRange?)?,
) {
    val range = documentSelection?.invoke() ?: return
    val start = range.min.coerceIn(0, length)
    val end = range.max.coerceIn(0, length)
    if (start < end) addStyle(styles.selectionHighlight, start, end)
}

/** Visual styles for rich spans, shared with the Markdown preview via [MarkdownInlineStyles]. */
@Immutable
data class RichSpanVisualStyles(
    val bold: SpanStyle,
    val italic: SpanStyle,
    val strikethrough: SpanStyle,
    val code: SpanStyle,
    val link: SpanStyle,
    val selectionHighlight: SpanStyle,
) {
    fun forStyle(style: RichSpanStyle): SpanStyle = when (style) {
        RichSpanStyle.Bold -> bold
        RichSpanStyle.Italic -> italic
        RichSpanStyle.Strikethrough -> strikethrough
        RichSpanStyle.Code -> code
        is RichSpanStyle.Link -> link
    }
}

@Composable
fun rememberRichSpanVisualStyles(): RichSpanVisualStyles {
    val colors = rememberMarkdownColors()
    return remember(colors) {
        RichSpanVisualStyles(
            bold = MarkdownInlineStyles.bold,
            italic = MarkdownInlineStyles.italic,
            strikethrough = MarkdownInlineStyles.strikethrough,
            code = MarkdownInlineStyles.code(colors),
            link = MarkdownInlineStyles.link(colors),
            // Same swatch as the editor's native field selection palette, so the
            // cross-block highlight is indistinguishable from single-field selection.
            selectionHighlight = SpanStyle(background = colors.text.copy(alpha = 0.3f)),
        )
    }
}

package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
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
) : InputTransformation {

    override fun TextFieldBuffer.transformInput() {
        if (changes.changeCount == 0) return
        if (onSplit != null && changes.changeCount == 1) {
            val newRange = changes.getRange(0)
            val inserted = asCharSequence().subSequence(newRange.min, newRange.max).toString()
            if (inserted == "\n") {
                val originalRange = changes.getOriginalRange(0)
                revertAllChanges()
                onSplit.invoke(originalRange.min, originalRange.max)
                return
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
    }
}

/** Applies a block's spans as visual styles; the text itself is never touched. */
class RichSpanOutputTransformation(
    private val state: RichTextBlockState,
    private val styles: RichSpanVisualStyles,
) : OutputTransformation {

    override fun TextFieldBuffer.transformOutput() {
        state.spans.forEach { span ->
            if (span.end <= length) {
                addStyle(styles.forStyle(span.style), span.start, span.end)
            }
        }
    }
}

/** Visual styles for rich spans, shared with the Markdown preview via [MarkdownInlineStyles]. */
@Immutable
data class RichSpanVisualStyles(
    val bold: SpanStyle,
    val italic: SpanStyle,
    val strikethrough: SpanStyle,
    val code: SpanStyle,
    val link: SpanStyle,
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
        )
    }
}

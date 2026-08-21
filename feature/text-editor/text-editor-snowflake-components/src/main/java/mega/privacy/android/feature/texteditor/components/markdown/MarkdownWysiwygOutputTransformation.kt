package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ParagraphStyle

/**
 * Styling-only WYSIWYG pass over raw Markdown source: content is styled (headings sized, bold
 * bolded, code monospaced...) and syntax delimiters are dimmed, while the text itself is never
 * mutated. Identity offset mapping means the cursor, selection, and IME composing region are
 * untouched — the raw source in the TextFieldState stays the single source of truth for
 * dirty-tracking, undo, and save.
 *
 * Headings additionally get their full source line sized and a matching paragraph line height,
 * so enlarged text is not clipped by the editor's fixed 20sp line height and the `#` prefix
 * scales with its heading instead of staying body-sized.
 */
internal class MarkdownWysiwygOutputTransformation(
    private val parseCache: MarkdownEditorParseCache,
    private val styles: MarkdownWysiwygStyles,
) : OutputTransformation {

    override fun TextFieldBuffer.transformOutput() {
        val text = asCharSequence().toString()
        val parseResult = parseCache.parse(text)
        parseResult.spans.forEach { span ->
            if (span.start >= span.end || span.end > length) return@forEach
            val kind = span.kind
            if (kind is MarkdownSpanKind.Heading) {
                addHeadingLineStyles(text, span, kind.level)
            } else {
                styles.spanStyleFor(kind)?.let { addStyle(it, span.start, span.end) }
            }
        }
    }

    private fun TextFieldBuffer.addHeadingLineStyles(text: String, span: MarkdownSpan, level: Int) {
        val lineStart = text.lastIndexOf('\n', span.start - 1) + 1
        val lineEnd = text.indexOf('\n', span.end).let { if (it == -1) text.length else it }
        if (lineStart >= lineEnd) return
        // Whole line (marker included) so the dimmed `#` prefix scales with its heading; the
        // delimiter span only overrides color, so the size from this style survives the merge.
        styles.headingSpanStyleFor(level)?.let { addStyle(it, lineStart, lineEnd) }
        styles.headingLineHeightFor(level)?.let {
            addStyle(ParagraphStyle(lineHeight = it), lineStart, lineEnd)
        }
    }
}

/**
 * Remembers a [MarkdownWysiwygOutputTransformation] with its own parse cache. Attach to the
 * editor's `BasicTextField(outputTransformation = ...)`; pass null instead to show raw source.
 */
@Composable
fun rememberMarkdownWysiwygOutputTransformation(): OutputTransformation {
    val styles = rememberMarkdownWysiwygStyles()
    return remember(styles) {
        MarkdownWysiwygOutputTransformation(MarkdownEditorParseCache(), styles)
    }
}

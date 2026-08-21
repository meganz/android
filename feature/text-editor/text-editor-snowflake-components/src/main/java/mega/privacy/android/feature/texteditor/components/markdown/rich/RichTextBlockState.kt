package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange

/**
 * Editing state of one rich block: the plain-text field plus its formatting spans and the
 * typing styles applied to the next inserted characters. [spans] and [typingStyles] are
 * snapshot state, so the span-applying output transformation restyles automatically.
 *
 * The span/typing mechanics live in [RichSpanAdjuster]; this class owns the mutable state and
 * exposes the operations the editor and toolbar need.
 */
class RichTextBlockState(
    initialText: String = "",
    initialSpans: List<RichSpan> = emptyList(),
) {
    val textFieldState = TextFieldState(initialText)

    var spans: List<RichSpan> by mutableStateOf(RichSpanAdjuster.normalize(initialSpans))
        internal set

    var typingStyles: Set<RichSpanStyle> by mutableStateOf(emptySet())
        private set

    /** Styles covering [offset] — a caret directly after styled text counts as inside it. */
    fun stylesAt(offset: Int): Set<RichSpanStyle> =
        spans.filter { offset > it.start && offset <= it.end }.map { it.style }.toSet()

    /**
     * Toolbar toggle: with a selection, applies/removes [style] over it; with a collapsed
     * cursor, flips the typing style so it affects what is typed next.
     */
    fun toggleStyle(style: RichSpanStyle, selection: TextRange = textFieldState.selection) {
        if (selection.collapsed) {
            typingStyles = if (style in typingStyles) {
                typingStyles - style
            } else {
                typingStyles + style
            }
        } else {
            spans = RichSpanAdjuster.toggle(spans, selection.min, selection.max, style)
        }
    }

    /** Realigns the typing styles with the caret position (call when the caret moves). */
    fun syncTypingStylesToCaret() {
        typingStyles = stylesAt(textFieldState.selection.min)
    }

    /** The block's content as model text. */
    fun toRichText(): RichText = RichText(textFieldState.text.toString(), spans)
}

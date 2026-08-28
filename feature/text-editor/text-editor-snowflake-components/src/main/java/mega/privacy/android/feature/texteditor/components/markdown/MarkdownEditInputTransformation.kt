package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.TextRange

/**
 * Industry-standard Markdown typing behaviors, applied to user input only (never programmatic
 * edits):
 *
 * - **Enter after a list/quote/task item** continues it on the new line — ordered numbers
 *   increment, task boxes reset to unchecked, indentation is preserved.
 * - **Enter on an empty item** exits the list: the bare marker and the new line are removed.
 * - **Backspace right after a marker** removes the whole marker instead of one character.
 *
 * Markdown shortcuts (`- `, `# `, `**bold**`) need no handling here: the raw text *is* Markdown,
 * so the output transformation styles them the moment the syntax is complete.
 */
@OptIn(ExperimentalFoundationApi::class)
object MarkdownEditInputTransformation : InputTransformation {

    override fun TextFieldBuffer.transformInput() {
        if (changes.changeCount != 1) return
        val range = changes.getRange(0)
        val originalRange = changes.getOriginalRange(0)
        when {
            range.length == 1 && originalRange.collapsed &&
                    asCharSequence()[range.min] == '\n' -> handleNewline(range.min)

            range.collapsed && originalRange.length == 1 ->
                handleMarkerBackspace(range.min, originalRange)
        }
    }

    private fun TextFieldBuffer.handleNewline(newlinePos: Int) {
        val text = asCharSequence()
        val lineStart = text.lineStartBefore(newlinePos)
        val previousLine = text.subSequence(lineStart, newlinePos).toString()
        val marker = MarkdownListContinuation.markerOf(previousLine) ?: return
        // Exit the list only when the whole original line was a bare marker; Enter right after
        // the marker of an item WITH content pushes the content down as a new item instead.
        val lineEnd = text.indexOf('\n', newlinePos + 1).let { if (it == -1) text.length else it }
        val restOfLine = text.subSequence(newlinePos + 1, lineEnd)
        if (MarkdownListContinuation.isEmptyItem(previousLine) && restOfLine.isBlank()) {
            replace(lineStart, newlinePos + 1, "")
            selection = TextRange(lineStart)
        } else {
            replace(newlinePos + 1, newlinePos + 1, marker.continuation)
            selection = TextRange(newlinePos + 1 + marker.continuation.length)
        }
    }

    private fun TextFieldBuffer.handleMarkerBackspace(caret: Int, originalRange: TextRange) {
        val text = asCharSequence()
        val lineStart = text.lineStartBefore(caret)
        if (caret <= lineStart) return
        // Everything before the caret is untouched by this single deletion, so [lineStart] maps
        // to the same offset in the original text; the original prefix ends where the deleted
        // character did. Trigger only when that prefix was exactly a complete marker.
        val originalPrefix = originalText.subSequence(lineStart, originalRange.max).toString()
        val marker = MarkdownListContinuation.markerOf(originalPrefix) ?: return
        if (marker.length != originalPrefix.length) return
        replace(lineStart, caret, "")
        selection = TextRange(lineStart)
    }

    private fun CharSequence.lineStartBefore(offset: Int): Int {
        for (i in offset - 1 downTo 0) {
            if (this[i] == '\n') return i + 1
        }
        return 0
    }
}

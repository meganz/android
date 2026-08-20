package mega.privacy.android.feature.texteditor.components.markdown

import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.StrongEmphasis

/**
 * Which Markdown formats cover the current cursor/selection; drives the formatting toolbar's
 * active (toggled) states. A format is active when the whole selection lies inside a node of
 * that type — for a collapsed cursor, when the cursor sits inside (or at the edge of) one.
 */
data class MarkdownSelectionFormats(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isInlineCode: Boolean = false,
    val isLink: Boolean = false,
    val headingLevel: Int? = null,
    val isBulletList: Boolean = false,
    val isOrderedList: Boolean = false,
    val isQuote: Boolean = false,
) {
    companion object {
        val Empty = MarkdownSelectionFormats()

        /**
         * Detects the formats covering [selectionStart]..[selectionEnd] (offsets into
         * [MarkdownEditorParseResult.text]; equal offsets = collapsed cursor).
         */
        fun from(
            parseResult: MarkdownEditorParseResult,
            selectionStart: Int,
            selectionEnd: Int,
        ): MarkdownSelectionFormats {
            var result = Empty
            fun visit(parent: Node) {
                var node = parent.firstChild
                while (node != null) {
                    if (node.covers(selectionStart, selectionEnd)) {
                        result = result.with(node)
                        visit(node)
                    }
                    node = node.next
                }
            }
            visit(parseResult.document)
            return result
        }

        private fun Node.covers(selectionStart: Int, selectionEnd: Int): Boolean {
            val spans = sourceSpans
            if (spans.isEmpty()) return false
            val last = spans.last()
            return selectionStart >= spans.first().inputIndex &&
                    selectionEnd <= last.inputIndex + last.length
        }

        private fun MarkdownSelectionFormats.with(node: Node): MarkdownSelectionFormats =
            when (node) {
                is StrongEmphasis -> copy(isBold = true)
                is Emphasis -> copy(isItalic = true)
                is Strikethrough -> copy(isStrikethrough = true)
                is Code -> copy(isInlineCode = true)
                is Link -> copy(isLink = true)
                is Heading -> copy(headingLevel = node.level)
                is BlockQuote -> copy(isQuote = true)
                is ListItem -> when (node.parent) {
                    is BulletList -> copy(isBulletList = true)
                    is OrderedList -> copy(isOrderedList = true)
                    else -> this
                }

                else -> this
            }
    }
}

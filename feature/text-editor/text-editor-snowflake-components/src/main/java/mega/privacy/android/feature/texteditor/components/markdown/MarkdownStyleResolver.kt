package mega.privacy.android.feature.texteditor.components.markdown

import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node.BlockQuote
import org.commonmark.node.Code
import org.commonmark.node.Delimited
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.SourceSpan
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.ThematicBreak

/**
 * Maps a CommonMark AST (parsed with `IncludeSourceSpans.BLOCKS_AND_INLINES`) to flat
 * [MarkdownSpan]s over the raw source text. Pure Kotlin — no Compose types — so the offset
 * logic is unit-testable in isolation. The WYSIWYG output transformation turns these spans
 * into `SpanStyle`s (and hides [MarkdownSpanKind.Delimiter] ranges in live preview).
 *
 * Constructs without an entry here (tables, HTML, images' alt text) intentionally stay
 * unstyled and edit as raw source.
 */
internal object MarkdownStyleResolver {

    fun resolve(text: String, document: Node): List<MarkdownSpan> {
        val spans = mutableListOf<MarkdownSpan>()
        visit(document, text, spans)
        spans.sortBy { it.start }
        return spans
    }

    private fun visit(parent: Node, text: String, out: MutableList<MarkdownSpan>) {
        var node = parent.firstChild
        while (node != null) {
            resolveNode(node, text, out)
            visit(node, text, out)
            node = node.next
        }
    }

    private fun resolveNode(node: Node, text: String, out: MutableList<MarkdownSpan>) {
        when (node) {
            is Heading -> resolveHeading(node, text, out)
            is StrongEmphasis -> resolveDelimited(node, node, MarkdownSpanKind.Bold, out)
            is Emphasis -> resolveDelimited(node, node, MarkdownSpanKind.Italic, out)
            is Strikethrough ->
                resolveDelimited(node, node, MarkdownSpanKind.Strikethrough, out)

            is Code -> resolveInlineCode(node, text, out)
            is Link -> resolveLink(node, out)
            is Image -> resolveImage(node, out)
            is FencedCodeBlock -> resolveFencedCodeBlock(node, out)
            is IndentedCodeBlock -> resolveIndentedCodeBlock(node, out)
            is BlockQuote -> resolveQuoteMarkers(node, text, out)
            is ListItem -> resolveListMarker(node, text, out)
            is ThematicBreak -> node.range()?.let { (start, end) ->
                out += MarkdownSpan(start, end, MarkdownSpanKind.ThematicBreak)
            }

            else -> Unit
        }
    }

    /** Bounding [start, end) character range of a node, or null when it has no source spans. */
    private fun Node.range(): Pair<Int, Int>? {
        val spans = sourceSpans
        if (spans.isEmpty()) return null
        val first = spans.first()
        val last = spans.last()
        return first.inputIndex to (last.inputIndex + last.length)
    }

    private val SourceSpan.endIndex: Int get() = inputIndex + length

    private fun resolveHeading(node: Heading, text: String, out: MutableList<MarkdownSpan>) {
        val spans = node.sourceSpans
        if (spans.isEmpty()) return
        val first = spans.first()
        val isAtx = text.getOrNull(skipSpaces(text, first.inputIndex, first.endIndex)) == '#'
        if (isAtx) {
            val start = first.inputIndex
            val end = first.endIndex
            var i = skipSpaces(text, start, end)
            while (i < end && text[i] == '#') i++
            i = skipSpaces(text, i, end)
            // Optional closing sequence: a trailing run of '#' preceded by a space.
            var contentEnd = end
            var j = end
            while (j > i && text[j - 1] == ' ') j--
            var k = j
            while (k > i && text[k - 1] == '#') k--
            if (k < j && (k == i || text[k - 1] == ' ')) {
                contentEnd = k
                while (contentEnd > i && text[contentEnd - 1] == ' ') contentEnd--
                out += MarkdownSpan(contentEnd, end, MarkdownSpanKind.Delimiter)
            }
            if (start < i) out += MarkdownSpan(start, i, MarkdownSpanKind.Delimiter)
            if (i < contentEnd) {
                out += MarkdownSpan(i, contentEnd, MarkdownSpanKind.Heading(node.level))
            }
        } else {
            // Setext heading: content line(s) followed by a '===' / '---' underline line.
            val underline = spans.last()
            out += MarkdownSpan(
                underline.inputIndex,
                underline.endIndex,
                MarkdownSpanKind.Delimiter
            )
            val contentEnd = spans[spans.size - 2].endIndex
            out += MarkdownSpan(
                spans.first().inputIndex,
                contentEnd,
                MarkdownSpanKind.Heading(node.level),
            )
        }
    }

    private fun resolveDelimited(
        node: Node,
        delimited: Delimited,
        kind: MarkdownSpanKind,
        out: MutableList<MarkdownSpan>,
    ) {
        val (start, end) = node.range() ?: return
        val open = delimited.openingDelimiter?.length ?: 0
        val close = delimited.closingDelimiter?.length ?: 0
        if (end - start < open + close) return
        if (open > 0) out += MarkdownSpan(start, start + open, MarkdownSpanKind.Delimiter)
        if (close > 0) out += MarkdownSpan(end - close, end, MarkdownSpanKind.Delimiter)
        if (start + open < end - close) {
            out += MarkdownSpan(start + open, end - close, kind)
        }
    }

    private fun resolveInlineCode(node: Code, text: String, out: MutableList<MarkdownSpan>) {
        val (start, end) = node.range() ?: return
        var ticks = 0
        while (start + ticks < end && text[start + ticks] == '`') ticks++
        if (ticks == 0 || end - start < 2 * ticks) return
        out += MarkdownSpan(start, start + ticks, MarkdownSpanKind.Delimiter)
        out += MarkdownSpan(end - ticks, end, MarkdownSpanKind.Delimiter)
        if (start + ticks < end - ticks) {
            out += MarkdownSpan(start + ticks, end - ticks, MarkdownSpanKind.InlineCode)
        }
    }

    private fun resolveLink(node: Link, out: MutableList<MarkdownSpan>) {
        val (start, end) = node.range() ?: return
        val contentRange = childrenRange(node)
        if (contentRange == null) {
            out += MarkdownSpan(start, end, MarkdownSpanKind.Delimiter)
            return
        }
        val (contentStart, contentEnd) = contentRange
        if (start < contentStart) out += MarkdownSpan(
            start,
            contentStart,
            MarkdownSpanKind.Delimiter
        )
        if (contentEnd < end) out += MarkdownSpan(contentEnd, end, MarkdownSpanKind.Delimiter)
        out += MarkdownSpan(
            contentStart,
            contentEnd,
            MarkdownSpanKind.Link(node.destination.orEmpty()),
        )
    }

    /** Images render as raw alt text in the editor; only the syntax punctuation is marked. */
    private fun resolveImage(node: Image, out: MutableList<MarkdownSpan>) {
        val (start, end) = node.range() ?: return
        val contentRange = childrenRange(node)
        if (contentRange == null) {
            out += MarkdownSpan(start, end, MarkdownSpanKind.Delimiter)
            return
        }
        val (contentStart, contentEnd) = contentRange
        if (start < contentStart) out += MarkdownSpan(
            start,
            contentStart,
            MarkdownSpanKind.Delimiter
        )
        if (contentEnd < end) out += MarkdownSpan(contentEnd, end, MarkdownSpanKind.Delimiter)
    }

    /** Bounding range of a node's children, or null when none carries source spans. */
    private fun childrenRange(parent: Node): Pair<Int, Int>? {
        var start = Int.MAX_VALUE
        var end = Int.MIN_VALUE
        var child = parent.firstChild
        while (child != null) {
            child.range()?.let { (s, e) ->
                if (s < start) start = s
                if (e > end) end = e
            }
            child = child.next
        }
        return if (start <= end) start to end else null
    }

    private fun resolveFencedCodeBlock(node: FencedCodeBlock, out: MutableList<MarkdownSpan>) {
        val spans = node.sourceSpans
        if (spans.isEmpty()) return
        val hasClosingFence = node.closingFenceLength != null && spans.size > 1
        spans.forEachIndexed { index, span ->
            val kind = when {
                index == 0 -> MarkdownSpanKind.Delimiter
                hasClosingFence && index == spans.lastIndex -> MarkdownSpanKind.Delimiter
                else -> MarkdownSpanKind.CodeBlock
            }
            out += MarkdownSpan(span.inputIndex, span.endIndex, kind)
        }
    }

    private fun resolveIndentedCodeBlock(node: IndentedCodeBlock, out: MutableList<MarkdownSpan>) {
        node.sourceSpans.forEach { span ->
            out += MarkdownSpan(span.inputIndex, span.endIndex, MarkdownSpanKind.CodeBlock)
        }
    }

    /**
     * One [MarkdownSpanKind.QuoteMarker] per quoted line. Nested quotes emit their own markers:
     * an inner BlockQuote's source spans start after the outer `>` prefix.
     */
    private fun resolveQuoteMarkers(
        node: BlockQuote,
        text: String,
        out: MutableList<MarkdownSpan>,
    ) {
        node.sourceSpans.forEach { span ->
            var i = skipSpaces(text, span.inputIndex, span.endIndex, max = 3)
            if (i < span.endIndex && text[i] == '>') {
                i++
                if (i < span.endIndex && text[i] == ' ') i++
                out += MarkdownSpan(span.inputIndex, i, MarkdownSpanKind.QuoteMarker)
            }
        }
    }

    private fun resolveListMarker(node: ListItem, text: String, out: MutableList<MarkdownSpan>) {
        val first = node.sourceSpans.firstOrNull() ?: return
        val start = first.inputIndex
        val end = first.endIndex
        var i = skipSpaces(text, start, end)
        when {
            i < end && text[i] in "-*+" -> i++
            i < end && text[i].isDigit() -> {
                while (i < end && text[i].isDigit()) i++
                if (i < end && (text[i] == '.' || text[i] == ')')) i++ else return
            }

            else -> return
        }
        i = skipSpaces(text, i, end)
        if (start < i) out += MarkdownSpan(start, i, MarkdownSpanKind.ListMarker)
    }

    private fun skipSpaces(text: String, from: Int, until: Int, max: Int = Int.MAX_VALUE): Int {
        var i = from
        while (i < until && i - from < max && text[i] == ' ') i++
        return i
    }
}

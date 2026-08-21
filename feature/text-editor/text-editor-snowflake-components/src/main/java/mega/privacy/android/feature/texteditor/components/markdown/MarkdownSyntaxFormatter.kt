package mega.privacy.android.feature.texteditor.components.markdown

import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node.Code
import org.commonmark.node.Delimited
import org.commonmark.node.Emphasis
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.StrongEmphasis

/** Inline styles the formatting toolbar can toggle, with the Markdown delimiter each inserts. */
enum class MarkdownInlineStyle(internal val delimiter: String) {
    Bold("**"),
    Italic("*"),
    Strikethrough("~~"),
    Code("`"),
}

/** A single text replacement; [start]..[end] (exclusive) is replaced by [text]. */
data class MarkdownReplacement(val start: Int, val end: Int, val text: String)

/**
 * The outcome of a formatting action: [replacements] (sorted by start, non-overlapping) plus the
 * selection to restore after applying them, expressed in post-edit offsets.
 */
data class MarkdownFormatEdit(
    val replacements: List<MarkdownReplacement>,
    val selectionStart: Int,
    val selectionEnd: Int,
)

/** An existing link covering the cursor, for pre-filling the link dialog. */
data class MarkdownLinkInfo(
    val text: String,
    val url: String,
    val start: Int,
    val end: Int,
)

/**
 * Pure formatting operations over raw Markdown source, driving the toolbar in both edit modes.
 * Inline toggles detect existing formatting via the CommonMark AST (source spans), never regex;
 * line-level operations (headings, lists, quotes) rewrite line prefixes directly.
 *
 * All offsets index the exact text that was parsed. Callers apply the returned replacements
 * back-to-front inside a single `TextFieldState.edit { }` so each action is one undo step.
 */
object MarkdownSyntaxFormatter {

    /**
     * Toggles [style] for the selection: unwraps the covering styled node when there is one,
     * otherwise wraps the selection (or inserts an empty delimiter pair at a collapsed cursor,
     * leaving the caret between the delimiters).
     */
    fun toggleInline(
        parse: MarkdownEditorParseResult,
        selectionStart: Int,
        selectionEnd: Int,
        style: MarkdownInlineStyle,
    ): MarkdownFormatEdit {
        val match = findInlineMatch(parse.document, selectionStart, selectionEnd, style)
        if (match != null) {
            val (start, end, open, close) = match
            return MarkdownFormatEdit(
                replacements = listOf(
                    MarkdownReplacement(start, start + open, ""),
                    MarkdownReplacement(end - close, end, ""),
                ),
                selectionStart = (selectionStart - open).coerceAtLeast(start),
                selectionEnd = (selectionEnd - open).coerceAtLeast(start),
            )
        }
        val delimiter = style.delimiter
        if (selectionStart == selectionEnd) {
            return MarkdownFormatEdit(
                replacements = listOf(
                    MarkdownReplacement(selectionStart, selectionStart, delimiter + delimiter),
                ),
                selectionStart = selectionStart + delimiter.length,
                selectionEnd = selectionStart + delimiter.length,
            )
        }
        // Emphasis flanked by whitespace does not parse; wrap the trimmed selection instead.
        var from = selectionStart
        var until = selectionEnd
        val text = parse.text
        while (from < until && text[from].isWhitespace()) from++
        while (until > from && text[until - 1].isWhitespace()) until--
        if (from == until) {
            return MarkdownFormatEdit(emptyList(), selectionStart, selectionEnd)
        }
        return MarkdownFormatEdit(
            replacements = listOf(
                MarkdownReplacement(from, from, delimiter),
                MarkdownReplacement(until, until, delimiter),
            ),
            selectionStart = from + delimiter.length,
            selectionEnd = until + delimiter.length,
        )
    }

    /**
     * Cycles the heading level of every line the selection touches: body -> H1 -> H2 -> H3 ->
     * body. The target level is decided by the first selected line and applied uniformly.
     */
    fun cycleHeading(text: String, selectionStart: Int, selectionEnd: Int): MarkdownFormatEdit {
        val lines = selectedLineRanges(text, selectionStart, selectionEnd)
        val firstLevel = headingLevelOf(text, lines.first())
        val targetLevel = when (firstLevel) {
            0 -> 1
            in 1..2 -> firstLevel + 1
            else -> 0
        }
        val targetPrefix = if (targetLevel == 0) "" else "#".repeat(targetLevel) + " "
        val replacements = lines.mapNotNull { line ->
            val prefixEnd = headingPrefixEnd(text, line)
            if (text.substring(line.first, prefixEnd) == targetPrefix) {
                null
            } else {
                MarkdownReplacement(line.first, prefixEnd, targetPrefix)
            }
        }
        return withRemappedSelection(replacements, selectionStart, selectionEnd)
    }

    /**
     * Toggles a bullet or ordered list over every line the selection touches. If all non-blank
     * selected lines already carry that marker type the markers are removed; otherwise every
     * non-blank line gets one (ordered lists numbered from 1), replacing any existing marker.
     */
    fun toggleList(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        ordered: Boolean,
    ): MarkdownFormatEdit {
        val lines = selectedLineRanges(text, selectionStart, selectionEnd)
            .filter { it.first != it.last + 1 && !isBlankLine(text, it) }
        if (lines.isEmpty()) return MarkdownFormatEdit(emptyList(), selectionStart, selectionEnd)
        val allMarked = lines.all { listMarkerEnd(text, it, ordered) != null }
        var number = 1
        val replacements = lines.mapNotNull { line ->
            if (allMarked) {
                val markerEnd = listMarkerEnd(text, line, ordered) ?: return@mapNotNull null
                MarkdownReplacement(line.first, markerEnd, "")
            } else {
                val existingEnd = listMarkerEnd(text, line, ordered = true)
                    ?: listMarkerEnd(text, line, ordered = false)
                    ?: line.first
                val marker = if (ordered) "${number++}. " else "- "
                if (text.substring(line.first, existingEnd) == marker) {
                    null
                } else {
                    MarkdownReplacement(line.first, existingEnd, marker)
                }
            }
        }
        return withRemappedSelection(replacements, selectionStart, selectionEnd)
    }

    /** Toggles a `> ` quote prefix on every line the selection touches. */
    fun toggleQuote(text: String, selectionStart: Int, selectionEnd: Int): MarkdownFormatEdit {
        val lines = selectedLineRanges(text, selectionStart, selectionEnd)
        val allQuoted = lines.all { isBlankLine(text, it) || quoteMarkerEnd(text, it) != null }
        val replacements = lines.mapNotNull { line ->
            if (allQuoted) {
                val markerEnd = quoteMarkerEnd(text, line) ?: return@mapNotNull null
                MarkdownReplacement(line.first, markerEnd, "")
            } else {
                if (isBlankLine(text, line)) null
                else MarkdownReplacement(line.first, line.first, "> ")
            }
        }
        return withRemappedSelection(replacements, selectionStart, selectionEnd)
    }

    /** The existing link covering the selection, or null. Used to pre-fill the link dialog. */
    fun linkAt(
        parse: MarkdownEditorParseResult,
        selectionStart: Int,
        selectionEnd: Int,
    ): MarkdownLinkInfo? {
        val link = findCovering(parse.document, selectionStart, selectionEnd) { it is Link }
                as? Link ?: return null
        val range = link.charRange() ?: return null
        val content = childrenCharRange(link)
        val linkText = if (content != null) {
            parse.text.substring(content.first, content.second)
        } else {
            ""
        }
        return MarkdownLinkInfo(
            text = linkText,
            url = link.destination.orEmpty(),
            start = range.first,
            end = range.second,
        )
    }

    /**
     * Inserts or updates a link. When [existing] is non-null its whole `[text](url)` range is
     * replaced; otherwise a non-collapsed selection is wrapped, and a collapsed cursor gets the
     * full link inserted. A blank [url] with an [existing] link unwraps it to plain text.
     */
    fun applyLink(
        selectionStart: Int,
        selectionEnd: Int,
        existing: MarkdownLinkInfo?,
        linkText: String,
        url: String,
    ): MarkdownFormatEdit {
        val replacement: MarkdownReplacement
        if (existing != null) {
            val newText = if (url.isBlank()) linkText else "[$linkText]($url)"
            replacement = MarkdownReplacement(existing.start, existing.end, newText)
        } else {
            replacement = MarkdownReplacement(
                selectionStart,
                selectionEnd,
                "[$linkText]($url)",
            )
        }
        val caret = replacement.start + replacement.text.length
        return MarkdownFormatEdit(listOf(replacement), caret, caret)
    }

    // ---- Inline node matching ----

    private data class InlineMatch(val start: Int, val end: Int, val open: Int, val close: Int)

    private fun findInlineMatch(
        document: Node,
        selectionStart: Int,
        selectionEnd: Int,
        style: MarkdownInlineStyle,
    ): InlineMatch? {
        val node = findCovering(document, selectionStart, selectionEnd) {
            when (style) {
                MarkdownInlineStyle.Bold -> it is StrongEmphasis
                MarkdownInlineStyle.Italic -> it is Emphasis
                MarkdownInlineStyle.Strikethrough -> it is Strikethrough
                MarkdownInlineStyle.Code -> it is Code
            }
        } ?: return null
        val (start, end) = node.charRange() ?: return null
        return if (node is Delimited) {
            InlineMatch(
                start = start,
                end = end,
                open = node.openingDelimiter?.length ?: 0,
                close = node.closingDelimiter?.length ?: 0,
            )
        } else {
            // Inline code: symmetric backtick runs; their length is what the node range adds
            // around the literal.
            val literalLength = (node as? Code)?.literal?.length ?: return null
            val ticks = ((end - start - literalLength) / 2).coerceAtLeast(0)
            if (ticks == 0) null else InlineMatch(start, end, ticks, ticks)
        }
    }

    /** Deepest node matching [predicate] whose source range covers the selection. */
    private fun findCovering(
        parent: Node,
        selectionStart: Int,
        selectionEnd: Int,
        predicate: (Node) -> Boolean,
    ): Node? {
        var found: Node? = null
        var node = parent.firstChild
        while (node != null) {
            val range = node.charRange()
            if (range != null && selectionStart >= range.first && selectionEnd <= range.second) {
                val deeper = findCovering(node, selectionStart, selectionEnd, predicate)
                if (deeper != null) return deeper
                if (predicate(node)) found = node
            } else if (range == null) {
                val deeper = findCovering(node, selectionStart, selectionEnd, predicate)
                if (deeper != null) return deeper
            }
            node = node.next
        }
        return found
    }

    private fun Node.charRange(): Pair<Int, Int>? {
        val spans = sourceSpans
        if (spans.isEmpty()) return null
        val last = spans.last()
        return spans.first().inputIndex to (last.inputIndex + last.length)
    }

    private fun childrenCharRange(parent: Node): Pair<Int, Int>? {
        var start = Int.MAX_VALUE
        var end = Int.MIN_VALUE
        var child = parent.firstChild
        while (child != null) {
            child.charRange()?.let { (s, e) ->
                if (s < start) start = s
                if (e > end) end = e
            }
            child = child.next
        }
        return if (start <= end) start to end else null
    }

    // ---- Line helpers ----

    /**
     * One range per source line the selection touches (newline excluded). A selection ending
     * exactly at a line start does not include that line, matching standard editor behavior.
     */
    private fun selectedLineRanges(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
    ): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var lineStart =
            if (selectionStart == 0) 0 else text.lastIndexOf('\n', selectionStart - 1) + 1
        while (true) {
            val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
            ranges += lineStart until lineEnd
            lineStart = lineEnd + 1
            if (lineStart > text.length || lineStart >= selectionEnd) break
        }
        return ranges
    }

    private fun isBlankLine(text: String, line: IntRange): Boolean =
        line.all { text[it].isWhitespace() }

    private fun lineEndExclusive(line: IntRange): Int = line.last + 1

    private fun headingLevelOf(text: String, line: IntRange): Int {
        var i = line.first
        val end = lineEndExclusive(line)
        var spaces = 0
        while (i < end && text[i] == ' ' && spaces < 3) {
            i++; spaces++
        }
        var level = 0
        while (i < end && text[i] == '#') {
            i++; level++
        }
        if (level == 0 || level > 6) return 0
        return if (i >= end || text[i] == ' ') level else 0
    }

    /** End offset (exclusive) of the heading prefix (`### ` incl. trailing spaces) or lineStart. */
    private fun headingPrefixEnd(text: String, line: IntRange): Int {
        if (headingLevelOf(text, line) == 0) return line.first
        var i = line.first
        val end = lineEndExclusive(line)
        while (i < end && text[i] == ' ') i++
        while (i < end && text[i] == '#') i++
        while (i < end && text[i] == ' ') i++
        return i
    }

    /** End offset of the list marker of the requested type on this line, or null when absent. */
    private fun listMarkerEnd(text: String, line: IntRange, ordered: Boolean): Int? {
        var i = line.first
        val end = lineEndExclusive(line)
        var spaces = 0
        while (i < end && text[i] == ' ' && spaces < 3) {
            i++; spaces++
        }
        if (ordered) {
            val digitsStart = i
            while (i < end && text[i].isDigit()) i++
            if (i == digitsStart) return null
            if (i >= end || (text[i] != '.' && text[i] != ')')) return null
            i++
        } else {
            if (i >= end || text[i] !in "-*+") return null
            i++
        }
        if (i >= end || text[i] != ' ') return null
        while (i < end && text[i] == ' ') i++
        return i
    }

    private fun quoteMarkerEnd(text: String, line: IntRange): Int? {
        var i = line.first
        val end = lineEndExclusive(line)
        var spaces = 0
        while (i < end && text[i] == ' ' && spaces < 3) {
            i++; spaces++
        }
        if (i >= end || text[i] != '>') return null
        i++
        if (i < end && text[i] == ' ') i++
        return i
    }

    /** Remaps the selection through [replacements] (which must be sorted and non-overlapping). */
    private fun withRemappedSelection(
        replacements: List<MarkdownReplacement>,
        selectionStart: Int,
        selectionEnd: Int,
    ): MarkdownFormatEdit {
        var newStart = selectionStart
        var newEnd = selectionEnd
        replacements.forEach { replacement ->
            val delta = replacement.text.length - (replacement.end - replacement.start)
            if (replacement.start <= selectionStart) {
                newStart = (newStart + delta).coerceAtLeast(replacement.start)
            }
            if (replacement.start <= selectionEnd) {
                newEnd = (newEnd + delta).coerceAtLeast(replacement.start)
            }
        }
        return MarkdownFormatEdit(replacements, newStart, newEnd.coerceAtLeast(newStart))
    }
}

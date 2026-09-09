package mega.privacy.android.feature.texteditor.components.markdown.rich

import mega.privacy.android.feature.texteditor.components.markdown.rich.RichDocumentToMarkdownConverter.Companion.SpanOrder
import javax.inject.Inject


/**
 * Serializes a [RichDocument] back to Markdown. Plain text is escaped so it survives a re-parse
 * unchanged (CommonMark backslash escapes for inline specials, plus line-leading block markers);
 * [RichBlock.RawSource] blocks are emitted verbatim — together with the converter's raw-source
 * fallback this keeps unsupported constructs byte-identical across load -> save.
 */
class RichDocumentToMarkdownConverter @Inject constructor() {

    fun convert(document: RichDocument): String {
        val parts = StringBuilder()
        var previous: RichBlock? = null
        var orderedCounters = mutableMapOf<Int, Int>()
        document.blocks.forEach { block ->
            val before = previous
            if (before != null) {
                parts.append(separatorBetween(before, block))
            }
            if (block !is RichBlock.ListItem || before !is RichBlock.ListItem) {
                orderedCounters = mutableMapOf()
            }
            parts.append(serializeBlock(block, orderedCounters))
            previous = block
        }
        return parts.toString()
    }

    // Quotes deliberately separate with a blank line like other blocks: a single newline
    // lazy-continues the next quote into the previous paragraph on re-parse, merging blocks.
    private fun separatorBetween(previous: RichBlock, next: RichBlock): String = when {
        previous is RichBlock.ListItem && next is RichBlock.ListItem -> "\n"
        else -> "\n\n"
    }

    private fun serializeBlock(block: RichBlock, orderedCounters: MutableMap<Int, Int>): String =
        when (block) {
            is RichBlock.Paragraph -> serializeInline(block.text, escapeLineStarts = true)

            is RichBlock.Heading ->
                "#".repeat(block.level.coerceIn(1, 6)) + " " +
                        serializeInline(singleLine(block.text), escapeLineStarts = false)

            is RichBlock.ListItem -> {
                val indent = "    ".repeat(block.indent.coerceAtLeast(0))
                // Deeper indents reset when a shallower item appears; same-level items count up.
                orderedCounters.keys.filter { it > block.indent }.forEach(orderedCounters::remove)
                val marker = if (block.ordered) {
                    // A run head's start number seeds the count. It is deliberately ignored
                    // mid-run (a live counter wins): CommonMark cannot express a restart in
                    // adjacent items anyway — a list not starting at 1 needs other content
                    // before it, which is exactly when the counter has been reset.
                    val runStart = block.startNumber?.minus(1)
                    val number = (orderedCounters[block.indent] ?: runStart ?: 0) + 1
                    orderedCounters[block.indent] = number
                    "$number. "
                } else {
                    orderedCounters.remove(block.indent)
                    "- "
                }
                val task = when (block.checked) {
                    null -> ""
                    true -> "[x] "
                    false -> "[ ] "
                }
                indent + marker + task +
                        serializeInline(singleLine(block.text), escapeLineStarts = false)
            }

            is RichBlock.Quote -> {
                val prefix = "> ".repeat(block.depth.coerceAtLeast(1))
                serializeInline(block.text, escapeLineStarts = false)
                    .split('\n')
                    .joinToString("\n") { prefix + it }
            }

            is RichBlock.CodeBlock -> {
                val fence = "`".repeat(maxOf(3, longestBacktickRun(block.code) + 1))
                fence + block.language.orEmpty() + "\n" + block.code + "\n" + fence
            }

            RichBlock.ThematicBreak -> "---"

            is RichBlock.RawSource -> block.source

            is RichBlock.Table -> serializeTable(block)
        }

    private fun serializeTable(table: RichBlock.Table): String {
        // GFM requires pipes inside cells escaped everywhere, code spans included.
        fun cell(text: RichText): String =
            serializeInline(singleLine(text), escapeLineStarts = false).replace("|", "\\|")

        fun line(cells: List<RichText>): String =
            cells.joinToString(" | ", prefix = "| ", postfix = " |") { cell(it) }

        val delimiter = table.alignments.joinToString(" | ", prefix = "| ", postfix = " |") {
            when (it) {
                RichTableAlignment.Left -> ":---"
                RichTableAlignment.Center -> ":---:"
                RichTableAlignment.Right -> "---:"
                null -> "---"
            }
        }
        return buildString {
            append(line(table.header))
            append('\n')
            append(delimiter)
            table.rows.forEach { row ->
                append('\n')
                append(line(row))
            }
        }
    }

    /** Headings and list items are single-line constructs; inner newlines become spaces. */
    private fun singleLine(text: RichText): RichText =
        if ('\n' in text.text) text.copy(text = text.text.replace('\n', ' ')) else text

    // ---- Inline serialization ----

    /**
     * Emits [text] with delimiters at span boundaries. Spans from the converter are properly
     * nested (they mirror the AST); partially overlapping spans from future editing are split
     * at the outer span's end.
     */
    private fun serializeInline(text: RichText, escapeLineStarts: Boolean): String {
        val out = StringBuilder()
        val spans = trimUnderlineNewlines(text.text, text.spans)
            .sortedWith(SpanOrder)
            .toMutableList()
        serializeRange(text.text, spans, 0, text.text.length, out)
        val result = out.toString()
        return if (escapeLineStarts) escapeBlockMarkers(result) else result
    }

    /**
     * An underline tag alone at a line start re-parses as an HTML BLOCK (CommonMark type 7)
     * and would restructure the paragraph, so spans never open before or close after a
     * newline; the newline itself needs no decoration anyway.
     */
    private fun trimUnderlineNewlines(text: String, spans: List<RichSpan>): List<RichSpan> =
        spans.mapNotNull { span ->
            if (span.style != RichSpanStyle.Underline) return@mapNotNull span
            var start = span.start
            var end = span.end.coerceAtMost(text.length)
            while (start < end && text[start] == '\n') start++
            while (end > start && text[end - 1] == '\n') end--
            if (start < end) RichSpan(start, end, span.style) else null
        }

    /**
     * [spans] must be sorted outer-first ([SpanOrder]): spans with equal ranges nest in list
     * order; a span partially overlapping its outer sibling is clamped to the sibling's end.
     */
    private fun serializeRange(
        text: String,
        spans: MutableList<RichSpan>,
        from: Int,
        until: Int,
        out: StringBuilder,
    ) {
        var cursor = from
        var i = 0
        while (i < spans.size) {
            val span = spans[i]
            if (span.start < cursor || span.start >= until) {
                i++
                continue
            }
            val end = span.end.coerceAtMost(until)
            out.append(escapeInline(text.substring(cursor, span.start)))
            val children = mutableListOf<RichSpan>()
            var j = i + 1
            var firstTail = -1
            while (j < spans.size && spans[j].start < end) {
                val child = spans[j]
                if (child.end <= end) {
                    children += child
                } else {
                    // A span crossing the outer boundary is split: the head nests here and
                    // the tail is re-queued, so its formatting survives past the boundary.
                    children += child.copy(end = end)
                    spans[j] = child.copy(start = end)
                    if (firstTail == -1) firstTail = j
                }
                j++
            }
            when (val style = span.style) {
                RichSpanStyle.Bold -> wrap(out, "**") {
                    serializeRange(text, children, span.start, end, out)
                }

                RichSpanStyle.Italic -> wrap(out, "*") {
                    serializeRange(text, children, span.start, end, out)
                }

                RichSpanStyle.Underline -> {
                    out.append("<u>")
                    serializeRange(text, children, span.start, end, out)
                    out.append("</u>")
                }

                RichSpanStyle.Strikethrough -> wrap(out, "~~") {
                    serializeRange(text, children, span.start, end, out)
                }

                RichSpanStyle.Code -> out.append(codeSpan(text.substring(span.start, end)))

                is RichSpanStyle.Link -> {
                    out.append('[')
                    serializeRange(text, children, span.start, end, out)
                    out.append("](").append(linkDestination(style.url)).append(')')
                }
            }
            cursor = end
            // Land on the first re-queued tail; fully consumed spans between it and j are
            // skipped by the start-before-cursor guard above.
            i = if (firstTail != -1) firstTail else j
        }
        out.append(escapeInline(text.substring(cursor, until)))
    }

    private inline fun wrap(out: StringBuilder, delimiter: String, content: () -> Unit) {
        out.append(delimiter)
        content()
        out.append(delimiter)
    }

    private fun codeSpan(content: String): String {
        val ticks = "`".repeat(longestBacktickRun(content) + 1)
        val needsPadding = content.startsWith("`") || content.endsWith("`") || content.isEmpty()
        return if (needsPadding) "$ticks $content $ticks" else "$ticks$content$ticks"
    }

    /** URLs with spaces or parentheses need the CommonMark `<...>` destination form. */
    private fun linkDestination(url: String): String =
        if (url.any { it == ' ' || it == '(' || it == ')' }) "<$url>" else url

    private fun longestBacktickRun(text: String): Int {
        var longest = 0
        var run = 0
        text.forEach { c ->
            run = if (c == '`') run + 1 else 0
            if (run > longest) longest = run
        }
        return longest
    }

    /** Backslash-escapes inline specials so plain text survives a re-parse unchanged. */
    private fun escapeInline(text: String): String {
        val out = StringBuilder(text.length)
        text.forEach { c ->
            if (c in INLINE_SPECIALS) out.append('\\')
            out.append(c)
        }
        return out.toString()
    }

    /** Escapes characters that would start a block construct at the beginning of a line. */
    private fun escapeBlockMarkers(text: String): String =
        text.split('\n').joinToString("\n") { line ->
            val first = line.firstOrNull { !it.isWhitespace() }
            val index = line.indexOfFirst { !it.isWhitespace() }
            when {
                first == null -> line
                first in LINE_START_SPECIALS ->
                    line.substring(0, index) + "\\" + line.substring(index)

                first.isDigit() -> {
                    val digitsEnd = (index until line.length).firstOrNull {
                        !line[it].isDigit()
                    } ?: line.length
                    if (digitsEnd < line.length && (line[digitsEnd] == '.' || line[digitsEnd] == ')')) {
                        line.substring(0, digitsEnd) + "\\" + line.substring(digitsEnd)
                    } else {
                        line
                    }
                }

                else -> line
            }
        }

    private companion object {
        val INLINE_SPECIALS = setOf('\\', '`', '*', '_', '~', '[', ']', '<')
        val LINE_START_SPECIALS = setOf('#', '>', '-', '+', '=')

        /** Outer-first ordering: earlier start wins, wider span wins on ties. */
        // The style rank breaks ties between spans covering the exact same range, so their
        // nesting order in the output is deterministic instead of following parse order
        // (which differs between `<u>**x**</u>` and `**<u>x</u>**` and would oscillate).
        val SpanOrder = compareBy<RichSpan>(
            { it.start },
            { -(it.end - it.start) },
            { styleRank(it.style) },
        )

        private fun styleRank(style: RichSpanStyle): Int = when (style) {
            is RichSpanStyle.Link -> 0
            RichSpanStyle.Underline -> 1
            RichSpanStyle.Bold -> 2
            RichSpanStyle.Italic -> 3
            RichSpanStyle.Strikethrough -> 4
            RichSpanStyle.Code -> 5
        }
    }
}

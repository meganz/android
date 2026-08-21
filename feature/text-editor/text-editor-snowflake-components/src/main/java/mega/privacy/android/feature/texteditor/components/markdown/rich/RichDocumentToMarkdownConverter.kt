package mega.privacy.android.feature.texteditor.components.markdown.rich

import mega.privacy.android.feature.texteditor.components.markdown.rich.RichDocumentToMarkdownConverter.Companion.SpanOrder


/**
 * Serializes a [RichDocument] back to Markdown. Plain text is escaped so it survives a re-parse
 * unchanged (CommonMark backslash escapes for inline specials, plus line-leading block markers);
 * [RichBlock.RawSource] blocks are emitted verbatim — together with the converter's raw-source
 * fallback this keeps unsupported constructs byte-identical across load -> save.
 */
class RichDocumentToMarkdownConverter {

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
                    val number = (orderedCounters[block.indent] ?: 0) + 1
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
        serializeRange(text.text, text.spans.sortedWith(SpanOrder), 0, text.text.length, out)
        val result = out.toString()
        return if (escapeLineStarts) escapeBlockMarkers(result) else result
    }

    /**
     * [spans] must be sorted outer-first ([SpanOrder]): spans with equal ranges nest in list
     * order; a span partially overlapping its outer sibling is clamped to the sibling's end.
     */
    private fun serializeRange(
        text: String,
        spans: List<RichSpan>,
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
            while (j < spans.size && spans[j].start < end) {
                children += if (spans[j].end <= end) spans[j] else spans[j].copy(end = end)
                j++
            }
            when (val style = span.style) {
                RichSpanStyle.Bold -> wrap(out, "**") {
                    serializeRange(text, children, span.start, end, out)
                }

                RichSpanStyle.Italic -> wrap(out, "*") {
                    serializeRange(text, children, span.start, end, out)
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
            i = j
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
        val SpanOrder = compareBy<RichSpan>({ it.start }, { -(it.end - it.start) })
    }
}

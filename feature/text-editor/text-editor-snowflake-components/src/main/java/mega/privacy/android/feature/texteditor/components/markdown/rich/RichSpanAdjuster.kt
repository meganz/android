package mega.privacy.android.feature.texteditor.components.markdown.rich

/**
 * Pure span maintenance for the rich editor: remaps [RichSpan] ranges through text edits and
 * applies/removes styles over selections. This is the engine that keeps formatting attached to
 * the right characters while the user types — the rich-editor equivalent of what delimiters do
 * in raw Markdown.
 *
 * Boundary semantics follow the Notes/Docs conventions:
 * - Typing strictly inside a styled range extends it.
 * - Typing at a range's edge extends it only when the style is in the active typing styles.
 * - Replacing a selection that starts or ends exactly at a range's edge keeps the style
 *   (select a bold word and retype it — it stays bold).
 * - Deleting a range's whole content removes the span.
 */
object RichSpanAdjuster {

    /**
     * One text change, in the coordinates [androidx.compose.foundation.text.input.TextFieldBuffer.ChangeList]
     * reports: the replaced range in the ORIGINAL text and the replacement range in the NEW text.
     * Changes must be ascending and non-overlapping (the ChangeList contract).
     */
    data class TextChange(
        val originalStart: Int,
        val originalEnd: Int,
        val newStart: Int,
        val newEnd: Int,
    ) {
        val isInsertion: Boolean get() = originalStart == originalEnd
    }

    /** Remaps [spans] through [changes], growing edge-adjacent spans for [typingStyles]. */
    fun adjust(
        spans: List<RichSpan>,
        changes: List<TextChange>,
        typingStyles: Set<RichSpanStyle> = emptySet(),
    ): List<RichSpan> {
        if (changes.isEmpty()) return spans
        val mapped = spans.mapNotNull { span ->
            val extend = span.style in typingStyles
            val start = mapStart(span.start, changes, extend)
            val end = mapEnd(span.end, changes, extend)
            if (start < end) RichSpan(start, end, span.style) else null
        }
        // Styled typing into plain text: inserted ranges get the active styles even when there
        // was no adjacent span to extend.
        val withTyping = mapped.toMutableList()
        changes.filter { it.newEnd > it.newStart }.forEach { change ->
            typingStyles.forEach { style ->
                val covered = withTyping.any {
                    it.style == style && it.start <= change.newStart && it.end >= change.newEnd
                }
                if (!covered) withTyping += RichSpan(change.newStart, change.newEnd, style)
            }
        }
        return normalize(withTyping)
    }

    /** Toggles [style] over [start]..[end]: removes it when fully covered, applies it otherwise. */
    fun toggle(
        spans: List<RichSpan>,
        start: Int,
        end: Int,
        style: RichSpanStyle,
    ): List<RichSpan> {
        if (start >= end) return spans
        return if (isCovered(spans, start, end, style)) {
            remove(spans, start, end, style)
        } else {
            normalize(spans + RichSpan(start, end, style))
        }
    }

    /** True when every character of [start]..[end] carries [style]. */
    fun isCovered(spans: List<RichSpan>, start: Int, end: Int, style: RichSpanStyle): Boolean {
        var cursor = start
        spans.filter { it.style == style }.sortedBy { it.start }.forEach { span ->
            if (span.start <= cursor && span.end > cursor) {
                cursor = span.end
                if (cursor >= end) return true
            }
        }
        return cursor >= end
    }

    private fun remove(
        spans: List<RichSpan>,
        start: Int,
        end: Int,
        style: RichSpanStyle,
    ): List<RichSpan> = spans.flatMap { span ->
        if (span.style != style || span.end <= start || span.start >= end) {
            listOf(span)
        } else {
            buildList {
                if (span.start < start) add(RichSpan(span.start, start, span.style))
                if (span.end > end) add(RichSpan(end, span.end, span.style))
            }
        }
    }

    private fun mapStart(position: Int, changes: List<TextChange>, extend: Boolean): Int {
        var delta = 0
        changes.forEach { change ->
            when {
                position < change.originalStart -> return position + delta

                position == change.originalStart -> return when {
                    !change.isInsertion -> change.newStart // replaced leading content keeps style
                    extend -> change.newStart
                    else -> change.newEnd
                }

                position <= change.originalEnd -> return change.newEnd

                else -> delta += (change.newEnd - change.newStart) -
                        (change.originalEnd - change.originalStart)
            }
        }
        return position + delta
    }

    private fun mapEnd(position: Int, changes: List<TextChange>, extend: Boolean): Int {
        var delta = 0
        changes.forEach { change ->
            when {
                position < change.originalStart -> return position + delta

                position > change.originalEnd ->
                    delta += (change.newEnd - change.newStart) -
                            (change.originalEnd - change.originalStart)

                position == change.originalEnd -> return when {
                    !change.isInsertion -> change.newEnd // replaced trailing content keeps style
                    extend -> change.newEnd
                    else -> change.newStart
                }

                else -> return change.newStart // originalStart <= position < originalEnd
            }
        }
        return position + delta
    }

    /** Sorts, merges overlapping/adjacent same-style spans, and drops empty ones. */
    fun normalize(spans: List<RichSpan>): List<RichSpan> {
        val result = mutableListOf<RichSpan>()
        spans
            .filter { it.start < it.end }
            .groupBy { it.style }
            .forEach { (_, styled) ->
                var current: RichSpan? = null
                styled.sortedBy { it.start }.forEach { span ->
                    val open = current
                    current = when {
                        open == null -> span
                        span.start <= open.end -> open.copy(end = maxOf(open.end, span.end))
                        else -> {
                            result += open
                            span
                        }
                    }
                }
                current?.let { result += it }
            }
        return result.sortedWith(compareBy({ it.start }, { -(it.end - it.start) }))
    }
}

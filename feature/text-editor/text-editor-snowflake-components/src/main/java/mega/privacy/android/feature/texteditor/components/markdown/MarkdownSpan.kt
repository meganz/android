package mega.privacy.android.feature.texteditor.components.markdown

/**
 * A styled region of raw Markdown source, expressed in character offsets into the exact text
 * that was parsed. Produced by [MarkdownStyleResolver] and consumed by the WYSIWYG output
 * transformation, which maps each [kind] to a visual style (and later hides [MarkdownSpanKind.Delimiter]
 * ranges outside the caret's block).
 *
 * Spans may nest/overlap (e.g. bold inside a heading); they are emitted sorted by [start].
 *
 * @property start Inclusive start offset.
 * @property end Exclusive end offset.
 */
data class MarkdownSpan(
    val start: Int,
    val end: Int,
    val kind: MarkdownSpanKind,
)

/** What a [MarkdownSpan] represents in the source. */
sealed interface MarkdownSpanKind {

    /** Heading content (ATX or setext), excluding the `#`/underline delimiters. */
    data class Heading(val level: Int) : MarkdownSpanKind

    /** Strong emphasis content, excluding the `**`/`__` delimiters. */
    data object Bold : MarkdownSpanKind

    /** Emphasis content, excluding the `*`/`_` delimiters. */
    data object Italic : MarkdownSpanKind

    /** GFM strikethrough content, excluding the `~~` delimiters. */
    data object Strikethrough : MarkdownSpanKind

    /** Inline code content, excluding the backtick delimiters. */
    data object InlineCode : MarkdownSpanKind

    /** Link text content, excluding the `[`, `](…)` syntax. */
    data class Link(val destination: String) : MarkdownSpanKind

    /** One source line of a fenced or indented code block (fence lines are [Delimiter]s). */
    data object CodeBlock : MarkdownSpanKind

    /** A list item marker, including its trailing spaces (`- `, `* `, `1. `…). */
    data object ListMarker : MarkdownSpanKind

    /** A block quote `>` marker (per line), including one following space when present. */
    data object QuoteMarker : MarkdownSpanKind

    /** A thematic break line (`---`, `***`…). */
    data object ThematicBreak : MarkdownSpanKind

    /**
     * Markdown syntax punctuation surrounding styled content (`**`, `#`, backticks, fences,
     * link brackets/URL…). Dimmed in styling-only mode; hidden in live preview outside the
     * caret's block.
     */
    data object Delimiter : MarkdownSpanKind
}

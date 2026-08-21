package mega.privacy.android.feature.texteditor.components.markdown.rich

/**
 * The rich editor's document model: a flat list of always-editable blocks. Block text is PLAIN —
 * no Markdown syntax exists inside the editor; inline formatting lives in [RichSpan]s and block
 * structure in the [RichBlock] types. Markdown exists only at the file boundary
 * (MarkdownToRichDocumentConverter on load, RichDocumentToMarkdownConverter on save).
 */
data class RichDocument(val blocks: List<RichBlock>) {
    companion object {
        val Empty = RichDocument(listOf(RichBlock.Paragraph(RichText.Empty)))
    }
}

sealed interface RichBlock {

    data class Paragraph(val text: RichText) : RichBlock

    data class Heading(val level: Int, val text: RichText) : RichBlock

    /**
     * One list item. Nesting is flattened into [indent] (0-based depth), the Notes/Docs model.
     *
     * @property checked Task-box state; null when the item is not a task.
     */
    data class ListItem(
        val ordered: Boolean,
        val indent: Int,
        val checked: Boolean?,
        val text: RichText,
    ) : RichBlock

    /** One quoted paragraph; [depth] > 1 for quotes inside quotes. */
    data class Quote(val depth: Int, val text: RichText) : RichBlock

    data class CodeBlock(val language: String?, val code: String) : RichBlock

    data object ThematicBreak : RichBlock

    /**
     * Any construct the rich editor does not model (tables, HTML, reference definitions,
     * paragraphs with images...). Kept verbatim and re-emitted byte-identically on save —
     * the model's losslessness guarantee for unsupported Markdown.
     */
    data class RawSource(val source: String) : RichBlock
}

/** Plain text plus inline formatting spans ([start] inclusive, [end] exclusive, may nest). */
data class RichText(
    val text: String,
    val spans: List<RichSpan> = emptyList(),
) {
    companion object {
        val Empty = RichText("")
    }
}

data class RichSpan(val start: Int, val end: Int, val style: RichSpanStyle)

sealed interface RichSpanStyle {
    data object Bold : RichSpanStyle
    data object Italic : RichSpanStyle
    data object Strikethrough : RichSpanStyle
    data object Code : RichSpanStyle
    data class Link(val url: String) : RichSpanStyle
}

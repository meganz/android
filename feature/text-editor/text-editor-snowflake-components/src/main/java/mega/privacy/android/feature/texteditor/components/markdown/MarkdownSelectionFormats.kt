package mega.privacy.android.feature.texteditor.components.markdown

/**
 * Which Markdown formats cover the current cursor/selection; drives the formatting toolbar's
 * active (toggled) states. Derived from the focused rich block's kind, spans, and typing styles.
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
    }
}

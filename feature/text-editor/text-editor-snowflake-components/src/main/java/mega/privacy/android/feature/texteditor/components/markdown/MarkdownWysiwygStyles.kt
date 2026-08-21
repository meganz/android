package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.TextUnit
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.feature.texteditor.components.MarkdownInlineStyles
import mega.privacy.android.feature.texteditor.components.markdownHeadingStyle
import mega.privacy.android.feature.texteditor.components.rememberMarkdownColors

/**
 * Visual styles the WYSIWYG editor applies over raw Markdown source. Built from the same
 * sources of truth as the read-only preview — [rememberMarkdownColors], [markdownHeadingStyle],
 * and [MarkdownInlineStyles] — so View and Edit modes cannot disagree on how shared constructs
 * look.
 *
 * @property headingSpanStyles Index = heading level - 1 (H1..H6).
 * @property headingLineHeights Line height per heading level, applied as a paragraph style over
 * the heading's full source line so enlarged text is not clipped by the editor's fixed line height.
 */
@Immutable
internal data class MarkdownWysiwygStyles(
    val headingSpanStyles: List<SpanStyle>,
    val headingLineHeights: List<TextUnit>,
    val bold: SpanStyle,
    val italic: SpanStyle,
    val strikethrough: SpanStyle,
    val inlineCode: SpanStyle,
    val link: SpanStyle,
    val codeBlock: SpanStyle,
    val listMarker: SpanStyle,
    val quoteMarker: SpanStyle,
    val thematicBreak: SpanStyle,
    val delimiter: SpanStyle,
) {
    fun headingSpanStyleFor(level: Int): SpanStyle? = headingSpanStyles.getOrNull(level - 1)

    fun headingLineHeightFor(level: Int): TextUnit? = headingLineHeights.getOrNull(level - 1)

    /** Style for non-heading kinds; heading kinds are handled per line by the transformation. */
    fun spanStyleFor(kind: MarkdownSpanKind): SpanStyle? = when (kind) {
        is MarkdownSpanKind.Heading -> headingSpanStyleFor(kind.level)
        MarkdownSpanKind.Bold -> bold
        MarkdownSpanKind.Italic -> italic
        MarkdownSpanKind.Strikethrough -> strikethrough
        MarkdownSpanKind.InlineCode -> inlineCode
        is MarkdownSpanKind.Link -> link
        MarkdownSpanKind.CodeBlock -> codeBlock
        MarkdownSpanKind.ListMarker -> listMarker
        MarkdownSpanKind.QuoteMarker -> quoteMarker
        MarkdownSpanKind.ThematicBreak -> thematicBreak
        MarkdownSpanKind.Delimiter -> delimiter
    }
}

@Composable
internal fun rememberMarkdownWysiwygStyles(): MarkdownWysiwygStyles {
    val colors = rememberMarkdownColors()
    val dimColor = DSTokens.colors.text.secondary
    val headingStyles = (1..6).map { markdownHeadingStyle(it) }
    return remember(colors, dimColor, headingStyles) {
        MarkdownWysiwygStyles(
            headingSpanStyles = headingStyles.map {
                SpanStyle(fontSize = it.fontSize, fontWeight = it.fontWeight)
            },
            headingLineHeights = headingStyles.map { it.lineHeight },
            bold = MarkdownInlineStyles.bold,
            italic = MarkdownInlineStyles.italic,
            strikethrough = MarkdownInlineStyles.strikethrough,
            inlineCode = MarkdownInlineStyles.code(colors),
            link = MarkdownInlineStyles.link(colors),
            codeBlock = MarkdownInlineStyles.code(colors),
            listMarker = SpanStyle(
                color = colors.accent,
                fontWeight = MarkdownInlineStyles.bold.fontWeight
            ),
            quoteMarker = SpanStyle(
                color = colors.accent,
                fontWeight = MarkdownInlineStyles.bold.fontWeight
            ),
            thematicBreak = SpanStyle(color = dimColor),
            delimiter = SpanStyle(color = dimColor),
        )
    }
}

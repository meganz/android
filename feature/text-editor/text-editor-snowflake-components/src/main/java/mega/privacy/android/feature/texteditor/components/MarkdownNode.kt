package mega.privacy.android.feature.texteditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.DSTokens
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as CmText
import org.commonmark.node.ThematicBreak

/**
 * Max characters rendered in a single [Text]. Long paragraphs / code lines are split into
 * multiple Texts so no single native text measurement (MeasuredText) blocks the main thread
 * (AND-23707). Mirrors the chunked editor's per-chunk char cap.
 */
private const val MAX_TEXT_MEASURE_CHARS = 50_000

/** Resolved colors for Markdown rendering, mapped from design tokens. */
internal data class MarkdownColors(
    val text: Color,
    val accent: Color,
    val codeBackground: Color,
    val divider: Color,
    val quoteBar: Color,
)

@Composable
internal fun rememberMarkdownColors(): MarkdownColors = MarkdownColors(
    text = DSTokens.colors.text.primary,
    accent = DSTokens.colors.text.accent,
    codeBackground = DSTokens.colors.background.surface1,
    divider = DSTokens.colors.border.subtle,
    quoteBar = DSTokens.colors.border.strong,
)

/**
 * Vertical spacing for a block. Headings get more space above (to separate sections) and little
 * below (to hug their following text); other blocks get a comfortable bottom gap. Nested blocks
 * (inside list items / quotes) use a tighter, uniform gap.
 */
private fun blockSpacing(node: Node, nested: Boolean): Modifier {
    if (nested) return Modifier.padding(bottom = 4.dp)
    return when (node) {
        is Heading -> Modifier.padding(top = if (node.level <= 2) 20.dp else 14.dp, bottom = 6.dp)
        is ThematicBreak -> Modifier.padding(vertical = 12.dp)
        else -> Modifier.padding(bottom = 12.dp)
    }
}

/** Renders a single Markdown block node. [nested] tightens spacing for list/quote children. */
@Composable
internal fun MarkdownBlock(
    node: Node,
    colors: MarkdownColors,
    modifier: Modifier = Modifier,
    nested: Boolean = false,
) {
    val m = modifier.fillMaxWidth().then(blockSpacing(node, nested))
    when (node) {
        is Heading -> Text(
            text = rememberInline(node, colors),
            style = headingStyle(node.level),
            color = colors.text,
            modifier = m,
        )

        is Paragraph -> ChunkedText(
            text = rememberInline(node, colors),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
            modifier = m,
        )

        is BulletList -> MarkdownList(node, ordered = false, colors = colors, modifier = m)
        is OrderedList -> MarkdownList(node, ordered = true, colors = colors, modifier = m)
        is FencedCodeBlock -> CodeBlock(node.literal.trimEnd('\n'), colors, m)
        is IndentedCodeBlock -> CodeBlock(node.literal.trimEnd('\n'), colors, m)
        is BlockQuote -> BlockQuoteBlock(node, colors, m)
        is ThematicBreak -> HorizontalDivider(color = colors.divider, modifier = m)
        is TableBlock -> MarkdownTable(node, colors, m)
        else -> Unit
    }
}

@Composable
private fun headingStyle(level: Int): TextStyle {
    val t = MaterialTheme.typography
    val base = when (level) {
        1 -> t.headlineSmall
        2 -> t.titleLarge
        3 -> t.titleMedium
        4 -> t.titleSmall
        5 -> t.bodyLarge
        else -> t.bodyMedium
    }
    return base.copy(fontWeight = FontWeight.Bold)
}

@Composable
private fun MarkdownList(
    list: Node,
    ordered: Boolean,
    colors: MarkdownColors,
    modifier: Modifier = Modifier,
) {
    val start = (list as? OrderedList)?.startNumber ?: 1
    Column(modifier = modifier.fillMaxWidth()) {
        var item = list.firstChild
        var index = start
        while (item != null) {
            if (item is ListItem) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        text = if (ordered) "$index. " else "•  ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text,
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        var child = item.firstChild
                        while (child != null) {
                            MarkdownBlock(child, colors, nested = true)
                            child = child.next
                        }
                    }
                }
                index++
            }
            item = item.next
        }
    }
}

@Composable
private fun CodeBlock(
    code: String,
    colors: MarkdownColors,
    modifier: Modifier = Modifier,
) {
    val codeStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.codeBackground, RoundedCornerShape(8.dp))
            .horizontalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        // Split very long code into multiple Texts so a single line can't ANR text measurement.
        var start = 0
        while (start < code.length || start == 0) {
            val end = (start + MAX_TEXT_MEASURE_CHARS).coerceAtMost(code.length)
            Text(text = code.substring(start, end), style = codeStyle, color = colors.text)
            if (end >= code.length) break
            start = end
        }
    }
}

/**
 * Renders [text] as a single [Text], or splits it into multiple stacked Texts when it exceeds
 * [MAX_TEXT_MEASURE_CHARS] so no one native measurement blocks the main thread. [subSequence]
 * preserves inline styling within each chunk.
 */
@Composable
private fun ChunkedText(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (text.length <= MAX_TEXT_MEASURE_CHARS) {
        Text(text = text, style = style, color = color, modifier = modifier)
        return
    }
    Column(modifier = modifier) {
        var start = 0
        while (start < text.length) {
            val end = (start + MAX_TEXT_MEASURE_CHARS).coerceAtMost(text.length)
            Text(text = text.subSequence(start, end), style = style, color = color)
            start = end
        }
    }
}

@Composable
private fun BlockQuoteBlock(
    quote: Node,
    colors: MarkdownColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.width(3.dp).padding(end = 8.dp),
            color = colors.quoteBar,
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            var child = quote.firstChild
            while (child != null) {
                MarkdownBlock(child, colors, nested = true)
                child = child.next
            }
        }
    }
}

/** Fixed per-column width; the whole table scrolls horizontally so wide tables aren't squeezed. */
private val TableCellWidth = 140.dp

@Composable
private fun MarkdownTable(
    table: Node,
    colors: MarkdownColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .border(1.dp, colors.divider, RoundedCornerShape(4.dp)),
    ) {
        var section = table.firstChild
        while (section != null) {
            when (section) {
                is TableHead, is TableBody -> {
                    var row = section.firstChild
                    while (row != null) {
                        if (row is TableRow) {
                            TableRowView(row, header = section is TableHead, colors = colors)
                            HorizontalDivider(color = colors.divider)
                        }
                        row = row.next
                    }
                }
            }
            section = section.next
        }
    }
}

@Composable
private fun TableRowView(
    row: TableRow,
    header: Boolean,
    colors: MarkdownColors,
) {
    Row {
        var cell = row.firstChild
        while (cell != null) {
            if (cell is TableCell) {
                Text(
                    text = rememberInline(cell, colors),
                    style = MaterialTheme.typography.bodyMedium.let {
                        if (header) it.copy(fontWeight = FontWeight.Bold) else it
                    },
                    color = colors.text,
                    modifier = Modifier.width(TableCellWidth).padding(8.dp),
                )
            }
            cell = cell.next
        }
    }
}

// ---- Inline rendering ----

@Composable
internal fun rememberInline(parent: Node, colors: MarkdownColors): AnnotatedString =
    remember(parent, colors) {
        buildAnnotatedString { appendInlineChildren(parent, colors) }
    }

private fun AnnotatedString.Builder.appendInlineChildren(parent: Node, colors: MarkdownColors) {
    var child = parent.firstChild
    while (child != null) {
        appendInline(child, colors)
        child = child.next
    }
}

private fun AnnotatedString.Builder.appendInline(node: Node, colors: MarkdownColors) {
    when (node) {
        is CmText -> append(node.literal)
        is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            appendInlineChildren(node, colors)
        }

        is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            appendInlineChildren(node, colors)
        }

        is Code -> withStyle(
            SpanStyle(fontFamily = FontFamily.Monospace, background = colors.codeBackground),
        ) { append(node.literal) }

        is Link -> withLink(
            LinkAnnotation.Url(
                url = node.destination,
                styles = TextLinkStyles(
                    SpanStyle(color = colors.accent, textDecoration = TextDecoration.Underline),
                ),
            ),
        ) { appendInlineChildren(node, colors) }

        is Image -> appendInlineChildren(node, colors) // v1: render alt text only, no remote load
        is SoftLineBreak -> append(" ")
        is HardLineBreak -> append("\n")
        else -> appendInlineChildren(node, colors)
    }
}

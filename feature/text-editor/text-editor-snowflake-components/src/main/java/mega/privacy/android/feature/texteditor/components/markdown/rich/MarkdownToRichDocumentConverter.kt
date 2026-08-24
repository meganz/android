package mega.privacy.android.feature.texteditor.components.markdown.rich

import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlInline
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
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser
import javax.inject.Inject

/**
 * Converts Markdown source into the rich editor's [RichDocument]. Supported constructs become
 * typed blocks with plain text + spans; anything the editor does not model — tables, HTML,
 * reference definitions, and any block containing images or inline HTML — becomes a
 * [RichBlock.RawSource] carrying its verbatim source slice, so it survives save byte-identical.
 */
class MarkdownToRichDocumentConverter @Inject constructor() {

    private val parser: Parser = Parser.builder()
        .extensions(
            listOf(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListItemsExtension.create(),
            ),
        )
        .includeSourceSpans(IncludeSourceSpans.BLOCKS)
        .build()

    fun convert(markdown: String): RichDocument {
        if (markdown.isBlank()) return RichDocument.Empty
        val document = parser.parse(markdown)
        val blocks = mutableListOf<RichBlock>()
        var node = document.firstChild
        while (node != null) {
            convertBlock(node, markdown, quoteDepth = 0, listIndent = 0, out = blocks)
            node = node.next
        }
        return if (blocks.isEmpty()) RichDocument.Empty else RichDocument(blocks)
    }

    private fun convertBlock(
        node: Node,
        source: String,
        quoteDepth: Int,
        listIndent: Int,
        out: MutableList<RichBlock>,
    ) {
        when (node) {
            is Heading -> out += convertSupportedOrRaw(node, source) {
                RichBlock.Heading(node.level, inlineText(node))
            }

            is Paragraph -> out += convertSupportedOrRaw(node, source) {
                if (quoteDepth > 0) {
                    RichBlock.Quote(quoteDepth, inlineText(node))
                } else {
                    RichBlock.Paragraph(inlineText(node))
                }
            }

            is BulletList -> convertList(node, source, ordered = false, quoteDepth, listIndent, out)
            is OrderedList -> convertList(node, source, ordered = true, quoteDepth, listIndent, out)

            is BlockQuote -> {
                // Only paragraph / nested-quote children are modeled; a quote containing
                // anything else (lists, code...) falls back whole to raw source.
                var probe = node.firstChild
                while (probe != null) {
                    if (probe !is Paragraph && probe !is BlockQuote) {
                        out += rawSource(node, source)
                        return
                    }
                    probe = probe.next
                }
                var child = node.firstChild
                while (child != null) {
                    convertBlock(child, source, quoteDepth + 1, listIndent, out)
                    child = child.next
                }
            }

            is FencedCodeBlock -> out += RichBlock.CodeBlock(
                language = node.info?.takeWhile { !it.isWhitespace() }?.ifBlank { null },
                code = node.literal.orEmpty().trimEnd('\n'),
            )

            is IndentedCodeBlock -> out += RichBlock.CodeBlock(
                language = null,
                code = node.literal.orEmpty().trimEnd('\n'),
            )

            is ThematicBreak -> out += RichBlock.ThematicBreak

            else -> out += rawSource(node, source)
        }
    }

    private fun convertList(
        list: Node,
        source: String,
        ordered: Boolean,
        quoteDepth: Int,
        listIndent: Int,
        out: MutableList<RichBlock>,
    ) {
        var item = list.firstChild
        while (item != null) {
            if (item is ListItem) {
                var checked: Boolean? = null
                var itemText = RichText.Empty
                var textTaken = false
                val remaining = mutableListOf<Node>()
                var child = item.firstChild
                while (child != null) {
                    when {
                        child is TaskListItemMarker -> checked =
                            (child as TaskListItemMarker).isChecked

                        child is Paragraph && !textTaken -> {
                            if (hasUnsupportedInline(child)) {
                                out += rawSource(list, source)
                                return
                            }
                            itemText = inlineText(child)
                            textTaken = true
                        }

                        else -> remaining += child
                    }
                    child = child.next
                }
                out += RichBlock.ListItem(ordered, listIndent, checked, itemText)
                remaining.forEach { extra ->
                    when (extra) {
                        is BulletList ->
                            convertList(extra, source, false, quoteDepth, listIndent + 1, out)

                        is OrderedList ->
                            convertList(extra, source, true, quoteDepth, listIndent + 1, out)

                        else -> convertBlock(extra, source, quoteDepth, listIndent, out)
                    }
                }
            }
            item = item.next
        }
    }

    private inline fun convertSupportedOrRaw(
        node: Node,
        source: String,
        supported: () -> RichBlock,
    ): RichBlock = if (hasUnsupportedInline(node)) rawSource(node, source) else supported()

    /** Images and inline HTML have no inline model; the whole block falls back to raw source. */
    private fun hasUnsupportedInline(parent: Node): Boolean {
        var child = parent.firstChild
        while (child != null) {
            if (child is Image || child is HtmlInline || hasUnsupportedInline(child)) return true
            child = child.next
        }
        return false
    }

    private fun rawSource(node: Node, source: String): RichBlock.RawSource {
        val spans = node.sourceSpans
        if (spans.isEmpty()) return RichBlock.RawSource("")
        val last = spans.last()
        return RichBlock.RawSource(
            source.substring(
                spans.first().inputIndex,
                last.inputIndex + last.length
            )
        )
    }

    private fun inlineText(parent: Node): RichText {
        val builder = StringBuilder()
        val spans = mutableListOf<RichSpan>()

        fun walk(node: Node) {
            when (node) {
                is CmText -> builder.append(node.literal)

                is Code -> appendStyled(builder, spans, RichSpanStyle.Code) {
                    builder.append(node.literal)
                }

                is SoftLineBreak, is HardLineBreak -> builder.append('\n')

                else -> {
                    val walkChildren = {
                        var child = node.firstChild
                        while (child != null) {
                            walk(child)
                            child = child.next
                        }
                    }
                    when (val style = spanStyleOf(node)) {
                        null -> walkChildren()
                        else -> appendStyled(builder, spans, style) { walkChildren() }
                    }
                }
            }
        }

        var child = parent.firstChild
        while (child != null) {
            walk(child)
            child = child.next
        }
        return RichText(builder.toString(), spans)
    }

    /** The span style an inline container node maps to, or null for transparent containers. */
    private fun spanStyleOf(node: Node): RichSpanStyle? = when (node) {
        is StrongEmphasis -> RichSpanStyle.Bold
        is Emphasis -> RichSpanStyle.Italic
        is Strikethrough -> RichSpanStyle.Strikethrough
        is Link -> RichSpanStyle.Link(node.destination.orEmpty())
        else -> null
    }

    private inline fun appendStyled(
        builder: StringBuilder,
        spans: MutableList<RichSpan>,
        style: RichSpanStyle,
        content: () -> Unit,
    ) {
        val start = builder.length
        content()
        if (builder.length > start) spans += RichSpan(start, builder.length, style)
    }
}

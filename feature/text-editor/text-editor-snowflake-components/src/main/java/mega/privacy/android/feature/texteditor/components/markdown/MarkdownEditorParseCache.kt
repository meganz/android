package mega.privacy.android.feature.texteditor.components.markdown

import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Node
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser

/**
 * One parse of the raw Markdown editor text.
 *
 * @property text The exact source that was parsed; all offsets index into it.
 * @property document CommonMark AST with block + inline source spans.
 * @property spans Flat styled regions, sorted by start offset (see [MarkdownStyleResolver]).
 * @property topLevelBlocks Character range of each top-level block, in document order. Used to
 * find the block containing the caret (live-preview reveal, toolbar block-format detection).
 */
data class MarkdownEditorParseResult(
    val text: String,
    val document: Node,
    val spans: List<MarkdownSpan>,
    val topLevelBlocks: List<MarkdownBlockRange>,
)

/**
 * Character range of one top-level block.
 *
 * @property start Inclusive start offset.
 * @property end Exclusive end offset.
 */
data class MarkdownBlockRange(val start: Int, val end: Int) {
    fun contains(offset: Int): Boolean = offset in start..end
}

/**
 * Parses editor text into a [MarkdownEditorParseResult], memoizing the last result so the
 * output transformation, formatting toolbar, and input transformation share one parse per edit.
 *
 * Documents are gated to a single edit chunk (<= 50k chars), so a synchronous parse stays in
 * single-digit milliseconds; asynchronous parsing would only add stale styling frames.
 *
 * Not thread-safe by design: all consumers run on the UI thread.
 */
class MarkdownEditorParseCache {

    private val parser: Parser = Parser.builder()
        .extensions(listOf(TablesExtension.create(), StrikethroughExtension.create()))
        .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
        .build()

    private var cachedResult: MarkdownEditorParseResult? = null

    fun parse(text: CharSequence): MarkdownEditorParseResult {
        val source = text.toString()
        cachedResult?.let { if (it.text == source) return it }
        val document = parser.parse(source)
        return MarkdownEditorParseResult(
            text = source,
            document = document,
            spans = MarkdownStyleResolver.resolve(source, document),
            topLevelBlocks = topLevelBlockRanges(document),
        ).also { cachedResult = it }
    }

    private fun topLevelBlockRanges(document: Node): List<MarkdownBlockRange> {
        val ranges = mutableListOf<MarkdownBlockRange>()
        var block = document.firstChild
        while (block != null) {
            val spans = block.sourceSpans
            if (spans.isNotEmpty()) {
                val last = spans.last()
                ranges += MarkdownBlockRange(
                    spans.first().inputIndex,
                    last.inputIndex + last.length
                )
            }
            block = block.next
        }
        return ranges
    }
}

package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Structural identity of a text-bearing block in the rich editor; drives its chrome and text
 * style. Mutable per block (the toolbar retypes blocks in place, e.g. paragraph -> heading).
 */
sealed interface RichBlockKind {
    data object Paragraph : RichBlockKind
    data class Heading(val level: Int) : RichBlockKind
    data class Item(val ordered: Boolean, val indent: Int, val checked: Boolean?) : RichBlockKind
    data class Quote(val depth: Int) : RichBlockKind
}

/** Editing state of one block in the rich editor. */
sealed interface RichBlockEditState

/** A text-bearing block: paragraph, heading, list item, or quote. Always directly editable. */
class RichTextBlockEditState(
    kind: RichBlockKind,
    val text: RichTextBlockState,
) : RichBlockEditState {
    var kind: RichBlockKind by mutableStateOf(kind)
}

/** A fenced/indented code block, edited as plain monospace text inside its box. */
class CodeBlockEditState(
    val language: String?,
    initialCode: String,
) : RichBlockEditState {
    val code = TextFieldState(initialCode)
}

class ThematicBreakEditState : RichBlockEditState

/** Unsupported Markdown kept verbatim; rendered read-only and re-emitted byte-identical. */
class RawSourceEditState(val source: String) : RichBlockEditState

/**
 * Editing state of a whole rich document: the observable block list plus which block holds
 * focus. Bridges the immutable [RichDocument] model to always-editable per-block state, and
 * back via [toDocument] for serialization, dirty checks, and save.
 */
class RichDocumentState(document: RichDocument) {

    val blocks = mutableStateListOf<RichBlockEditState>().apply {
        document.blocks.forEach { add(it.toEditState()) }
    }

    var focusedIndex: Int? by mutableStateOf(null)

    /** The focused text-bearing block, or null (no focus, or a non-text block). */
    val focusedTextBlock: RichTextBlockEditState?
        get() = focusedIndex?.let { blocks.getOrNull(it) } as? RichTextBlockEditState

    fun toDocument(): RichDocument = RichDocument(
        blocks.map { block ->
            when (block) {
                is RichTextBlockEditState -> when (val kind = block.kind) {
                    RichBlockKind.Paragraph -> RichBlock.Paragraph(block.text.toRichText())
                    is RichBlockKind.Heading ->
                        RichBlock.Heading(kind.level, block.text.toRichText())

                    is RichBlockKind.Item -> RichBlock.ListItem(
                        ordered = kind.ordered,
                        indent = kind.indent,
                        checked = kind.checked,
                        text = block.text.toRichText(),
                    )

                    is RichBlockKind.Quote ->
                        RichBlock.Quote(kind.depth, block.text.toRichText())
                }

                is CodeBlockEditState ->
                    RichBlock.CodeBlock(block.language, block.code.text.toString())

                is ThematicBreakEditState -> RichBlock.ThematicBreak
                is RawSourceEditState -> RichBlock.RawSource(block.source)
            }
        },
    )
}

private fun RichBlock.toEditState(): RichBlockEditState = when (this) {
    is RichBlock.Paragraph ->
        RichTextBlockEditState(RichBlockKind.Paragraph, text.toBlockState())

    is RichBlock.Heading ->
        RichTextBlockEditState(RichBlockKind.Heading(level), text.toBlockState())

    is RichBlock.ListItem -> RichTextBlockEditState(
        RichBlockKind.Item(ordered, indent, checked),
        text.toBlockState(),
    )

    is RichBlock.Quote ->
        RichTextBlockEditState(RichBlockKind.Quote(depth), text.toBlockState())

    is RichBlock.CodeBlock -> CodeBlockEditState(language, code)
    is RichBlock.ThematicBreak -> ThematicBreakEditState()
    is RichBlock.RawSource -> RawSourceEditState(source)
}

private fun RichText.toBlockState(): RichTextBlockState = RichTextBlockState(text, spans)

package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import mega.privacy.android.feature.texteditor.components.MarkdownBlock
import mega.privacy.android.feature.texteditor.components.MarkdownBlockSpacing
import mega.privacy.android.feature.texteditor.components.MarkdownCodeFrame
import mega.privacy.android.feature.texteditor.components.MarkdownColors
import mega.privacy.android.feature.texteditor.components.MarkdownListItemChrome
import mega.privacy.android.feature.texteditor.components.MarkdownListMarker
import mega.privacy.android.feature.texteditor.components.MarkdownQuoteChrome
import mega.privacy.android.feature.texteditor.components.markdownHeadingStyle
import mega.privacy.android.feature.texteditor.components.parseTopLevelBlocks
import mega.privacy.android.feature.texteditor.components.rememberMarkdownColors
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

const val RICH_DOCUMENT_EDITOR_TAG = "rich_document_editor"

fun richBlockFieldTag(index: Int): String = "rich_document_editor:block_$index"

const val RICH_DOCUMENT_TAIL_TAP_TAG = "rich_document_editor:tail_tap_area"

private val TailTapAreaHeight = 160.dp

/** Idle time after which a typing burst is checkpointed for undo. */
private val UndoCheckpointDebounce = 1.seconds

/**
 * The rich editor surface: every block is always directly editable — no Markdown syntax, no
 * tap-to-reveal. Block structure (bullets, numbers, checkboxes, quote bars, code boxes) is
 * chrome drawn outside the text fields; inline formatting is applied by the span engine.
 */
@OptIn(FlowPreview::class)
@Composable
fun RichDocumentEditor(
    state: RichDocumentState,
    modifier: Modifier = Modifier,
) {
    val visualStyles = rememberRichSpanVisualStyles()
    val colors = rememberMarkdownColors()
    val focusRequesters = remember(state) { mutableMapOf<Int, FocusRequester>() }

    // Enter is only recorded by the input transformation (which runs in the text input
    // pipeline, off the composition applier); the actual block mutation happens here, on the
    // UI thread, after the field's edit session has committed.
    LaunchedEffect(state) {
        snapshotFlow { state.pendingSplit }.collect { request ->
            if (request == null) return@collect
            state.pendingSplit = null
            state.splitBlock(request.index, request.start, request.end)
        }
    }

    // Checkpoint typing bursts for undo once input settles, so undo steps back through typing
    // instead of jumping to the previous structural operation. Code blocks have no input
    // transformation, so their text length stands in for an edit counter.
    LaunchedEffect(state) {
        snapshotFlow {
            state.blocks.sumOf { block ->
                when (block) {
                    is RichTextBlockEditState -> block.text.editCount
                    is CodeBlockEditState -> block.code.text.length
                    else -> 0
                }
            }
        }
            .drop(1)
            .debounce(UndoCheckpointDebounce)
            .collect { state.recordUndo() }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.pendingFocus }.collect { request ->
            if (request == null) return@collect
            // Let the frame that composes a freshly split/merged block land first, so its
            // focus requester is attached before it is used.
            withFrameNanos { }
            val block = state.blocks.getOrNull(request.index) as? RichTextBlockEditState
            block?.text?.textFieldState?.edit {
                selection = TextRange(
                    request.selection.start.coerceIn(0, length),
                    request.selection.end.coerceIn(0, length),
                )
            }
            runCatching { focusRequesters[request.index]?.requestFocus() }
                .onFailure { Timber.w(it, "Pending focus request failed") }
            state.pendingFocus = null
        }
    }

    // Same selection palette as the preview and the chunked source editor, instead of the
    // Material default, so selecting text looks the same on every editor surface.
    val selectionColors = remember(colors.text) {
        TextSelectionColors(
            handleColor = colors.text,
            backgroundColor = colors.text.copy(alpha = 0.3f),
        )
    }
    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        RichDocumentBlocks(state, visualStyles, colors, focusRequesters, modifier)
    }
}

@Composable
private fun RichDocumentBlocks(
    state: RichDocumentState,
    visualStyles: RichSpanVisualStyles,
    colors: MarkdownColors,
    focusRequesters: MutableMap<Int, FocusRequester>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(RICH_DOCUMENT_EDITOR_TAG),
        // No top padding: matches the chunked source editor so switching edit modes does not
        // shift the content down (the collapsing top bar already provides the top spacing).
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        items(count = state.blocks.size, key = { it }) { index ->
            when (val block = state.blocks[index]) {
                is RichTextBlockEditState -> RichTextBlockRow(
                    index = index,
                    block = block,
                    state = state,
                    visualStyles = visualStyles,
                    colors = colors,
                    focusRequester = focusRequesters.getOrPut(index) { FocusRequester() },
                )

                is CodeBlockEditState -> CodeBlockRow(index, block, state, colors)

                is ThematicBreakEditState -> HorizontalDivider(
                    color = colors.divider,
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                is RawSourceEditState -> RawSourceRow(block, colors)
            }
        }
        item(key = "tail-tap-area") {
            Spacer(
                modifier = Modifier
                    .fillParentMaxWidth()
                    .height(TailTapAreaHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        val lastText = state.blocks.indexOfLast { it is RichTextBlockEditState }
                        if (lastText >= 0) {
                            runCatching { focusRequesters[lastText]?.requestFocus() }
                                .onFailure { Timber.w(it, "Tail focus request failed") }
                        }
                    }
                    .testTag(RICH_DOCUMENT_TAIL_TAP_TAG),
            )
        }
    }
}

@Composable
private fun RichTextBlockRow(
    index: Int,
    block: RichTextBlockEditState,
    state: RichDocumentState,
    visualStyles: RichSpanVisualStyles,
    colors: MarkdownColors,
    focusRequester: FocusRequester,
) {
    val kind = block.kind
    // Typing styles follow the caret (Notes behavior: moving the caret resets pending toggles;
    // typing keeps the style because the fresh span already covers the new caret position).
    LaunchedEffect(block.text) {
        snapshotFlow { block.text.textFieldState.selection }
            .collect { block.text.syncTypingStylesToCaret() }
    }
    // One stable Row for every kind: retyping a block (toolbar toggles, Enter on an empty
    // item, backspace demotion) only swaps the chrome sibling. Putting the field inside
    // per-kind wrappers would recreate it on a kind change, dropping focus and the keyboard.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(textRowPadding(state.blocks, index, kind)),
        verticalAlignment = Alignment.Top,
    ) {
        when (kind) {
            is RichBlockKind.Item -> MarkdownListItemChrome(
                marker = when {
                    kind.checked != null -> MarkdownListMarker.TaskCheckbox(
                        checked = kind.checked,
                        onCheckedChange = { state.setTaskChecked(index, it) },
                    )

                    kind.ordered -> MarkdownListMarker.Number(orderedNumber(state.blocks, index))
                    else -> MarkdownListMarker.Bullet
                },
                colors = colors,
                indent = kind.indent,
            )

            is RichBlockKind.Quote -> MarkdownQuoteChrome(depth = kind.depth, colors = colors)

            RichBlockKind.Paragraph, is RichBlockKind.Heading -> Unit
        }
        BasicTextField(
            state = block.text.textFieldState,
            textStyle = blockTextStyle(kind, colors),
            cursorBrush = SolidColor(colors.text),
            inputTransformation = remember(block, index) {
                RichSpanInputTransformation(block.text) { start, end ->
                    state.requestSplit(index, start, end)
                }
            },
            outputTransformation = remember(block, visualStyles) {
                RichSpanOutputTransformation(block.text, visualStyles)
            },
            modifier = Modifier
                .weight(1f)
                // Inside a continuous quote the paragraph gap moves INSIDE the row, so the
                // full-height bar spans it — one unbroken bar, like the preview's frame.
                .then(
                    if (kind is RichBlockKind.Quote &&
                        kindAt(state.blocks, index + 1) is RichBlockKind.Quote
                    ) {
                        Modifier.padding(MarkdownBlockSpacing.nested)
                    } else {
                        Modifier
                    },
                )
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) state.focusedIndex = index }
                .onPreviewKeyEvent { event ->
                    when {
                        handleUndoRedoKey(event, state) -> true

                        event.type == KeyEventType.KeyDown &&
                                event.key == Key.Backspace &&
                                block.text.textFieldState.selection == TextRange.Zero ->
                            state.mergeBlockBackward(index)

                        else -> false
                    }
                }
                .testTag(richBlockFieldTag(index)),
        )
    }
}

@Composable
private fun CodeBlockRow(
    index: Int,
    block: CodeBlockEditState,
    state: RichDocumentState,
    colors: MarkdownColors,
) {
    MarkdownCodeFrame(
        colors = colors,
        modifier = Modifier.padding(MarkdownBlockSpacing.block),
    ) {
        BasicTextField(
            state = block.code,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                color = colors.text,
            ),
            cursorBrush = SolidColor(colors.text),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) state.focusedIndex = index }
                .onPreviewKeyEvent { handleUndoRedoKey(it, state) }
                .testTag(richBlockFieldTag(index)),
        )
    }
}

@Composable
private fun RawSourceRow(block: RawSourceEditState, colors: MarkdownColors) {
    // Rendered through the preview's block components so unsupported constructs (tables,
    // HTML, images) look exactly like View mode. Not editable — the verbatim source slice
    // still serializes untouched, which is what keeps them lossless. The SelectionContainer
    // keeps this content copyable like every other block (the editable blocks get selection
    // from their text fields).
    val nodes = remember(block.source) { parseTopLevelBlocks(block.source) }
    SelectionContainer {
        Column(modifier = Modifier.fillMaxWidth()) {
            nodes.forEach { node ->
                MarkdownBlock(node = node, colors = colors)
            }
        }
    }
}

@Composable
private fun blockTextStyle(kind: RichBlockKind, colors: MarkdownColors): TextStyle = when (kind) {
    is RichBlockKind.Heading -> markdownHeadingStyle(kind.level).copy(color = colors.text)
    else -> MaterialTheme.typography.bodyMedium.copy(color = colors.text)
}

/**
 * Routes Ctrl+Z / Ctrl+Shift+Z / Ctrl+Y (hardware keyboards) to document-level undo/redo.
 * Always consumed, even when there is nothing to undo: the field's built-in text undo bypasses
 * the span engine and would desync spans from text.
 */
private fun handleUndoRedoKey(event: KeyEvent, state: RichDocumentState): Boolean {
    if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return false
    return when (event.key) {
        Key.Z -> {
            if (event.isShiftPressed) state.redo() else state.undo()
            true
        }

        Key.Y -> {
            state.redo()
            true
        }

        else -> false
    }
}

/**
 * Row padding mirroring the preview's rhythm: flattened list/quote rows stay tight, the LAST
 * row of a run carries the preview's block gap, and adjacent quote rows drop the outer gap
 * entirely so their bars join into one continuous bar.
 */
private fun textRowPadding(
    blocks: List<RichBlockEditState>,
    index: Int,
    kind: RichBlockKind,
): PaddingValues = when (kind) {
    is RichBlockKind.Heading -> MarkdownBlockSpacing.heading(kind.level)
    RichBlockKind.Paragraph -> MarkdownBlockSpacing.block

    is RichBlockKind.Item -> {
        val next = kindAt(blocks, index + 1) as? RichBlockKind.Item
        // A different list type at the SAME indent starts a new list; a deeper or shallower
        // neighbor is nesting within the same run and stays tight, like the preview.
        val endsRun = next == null || (next.indent == kind.indent && next.ordered != kind.ordered)
        val rowGap = if (kind.checked != null) {
            MarkdownBlockSpacing.taskRowGap
        } else {
            MarkdownBlockSpacing.listOrQuoteGap
        }
        PaddingValues(
            top = rowGap,
            bottom = if (endsRun) MarkdownBlockSpacing.runEndGap else rowGap,
        )
    }

    is RichBlockKind.Quote -> PaddingValues(
        top = if (kindAt(blocks, index - 1) is RichBlockKind.Quote) {
            0.dp
        } else {
            MarkdownBlockSpacing.listOrQuoteGap
        },
        bottom = if (kindAt(blocks, index + 1) is RichBlockKind.Quote) {
            0.dp
        } else {
            MarkdownBlockSpacing.runEndGap
        },
    )
}

private fun kindAt(blocks: List<RichBlockEditState>, index: Int): RichBlockKind? =
    (blocks.getOrNull(index) as? RichTextBlockEditState)?.kind

/**
 * 1-based number of an ordered item: counts ordered predecessors at the same indent, offset by
 * the run's start number when its first item carries one.
 */
private fun orderedNumber(blocks: List<RichBlockEditState>, index: Int): Int {
    val kind = (blocks.getOrNull(index) as? RichTextBlockEditState)?.kind
            as? RichBlockKind.Item ?: return 1
    var start = kind.startNumber ?: 1
    var offset = 0
    for (i in index - 1 downTo 0) {
        val previous = (blocks.getOrNull(i) as? RichTextBlockEditState)?.kind
                as? RichBlockKind.Item ?: break
        when {
            previous.indent < kind.indent -> break
            previous.indent > kind.indent -> continue
            previous.ordered -> {
                offset++
                previous.startNumber?.let { start = it }
            }

            else -> break
        }
    }
    return start + offset
}

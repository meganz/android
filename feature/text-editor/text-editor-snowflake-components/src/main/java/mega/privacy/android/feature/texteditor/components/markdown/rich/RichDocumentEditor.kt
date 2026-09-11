package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

/** Cell tag; row 0 is the header row, body rows start at 1. */
fun richTableCellTag(block: Int, row: Int, column: Int): String =
    "rich_document_editor:block_${block}_cell_${row}_$column"

private val TailTapAreaHeight = 160.dp

/** Idle time after which a typing burst is checkpointed for undo. */
private val UndoCheckpointDebounce = 1.seconds

/**
 * The rich editor surface: every block is always directly editable — no Markdown syntax, no
 * tap-to-reveal. Block structure (bullets, numbers, checkboxes, quote bars, code boxes) is
 * chrome drawn outside the text fields; inline formatting is applied by the span engine.
 *
 * @param autoFocusWhenEmpty Focus the first block and open the keyboard when the document is
 * a single empty block (a freshly created file); an existing document is never grabbed.
 */
@OptIn(FlowPreview::class)
@Composable
fun RichDocumentEditor(
    state: RichDocumentState,
    modifier: Modifier = Modifier,
    autoFocusWhenEmpty: Boolean = false,
) {
    val visualStyles = rememberRichSpanVisualStyles()
    val colors = rememberMarkdownColors()
    val focusRequesters = remember(state) { mutableMapOf<Int, FocusRequester>() }
    val geometry = remember(state) { RichSelectionGeometry() }
    val textToolbar = LocalTextToolbar.current
    val clipboard = LocalClipboard.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Edge autoscroll for selection drags: while a drag parks near the viewport edge, scroll
    // a step every frame and keep extending the selection toward the stationary finger.
    LaunchedEffect(state, geometry, listState) {
        snapshotFlow { geometry.autoscroll != null }.collectLatest { active ->
            if (!active) return@collectLatest
            while (isActive) {
                withFrameNanos { }
                val request = geometry.autoscroll ?: break
                listState.dispatchRawDelta(request.velocity)
                geometry.docPositionAt(state, request.dragRoot)?.let(request.moveEdge)
            }
        }
    }

    LaunchedEffect(state) {
        val single = state.blocks.singleOrNull() as? RichTextBlockEditState
        if (autoFocusWhenEmpty && single != null &&
            single.text.textFieldState.text.isEmpty()
        ) {
            state.pendingFocus = RichFocusRequest(0, TextRange.Zero)
        }
    }

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
                    is TableBlockEditState -> block.allCells.sumOf { it.editCount }
                    else -> 0
                }
            }
        }
            .drop(1)
            .debounce(UndoCheckpointDebounce)
            .collect { state.recordUndo() }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.pendingSmartInsert }.collect { request ->
            if (request == null) return@collect
            state.pendingSmartInsert = null
            state.smartInsert(request.index, request.start, request.end, request.text)
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.pendingSelectionReplacement }.collect { replacement ->
            if (replacement == null) return@collect
            state.pendingSelectionReplacement = null
            textToolbar.hide()
            state.replaceDocumentSelection(replacement)
        }
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

    // Any content or structure change invalidates the cross-block selection, exactly like a
    // native field selection dies on edit. Deliberately keyed on the text itself rather than
    // edit counters: the IME pushes no-op edits (re-committing unchanged text when the
    // selection moves), which must not wipe a selection the user just made.
    LaunchedEffect(state) {
        snapshotFlow {
            var signature = state.blocks.size
            fun add(text: CharSequence?) {
                signature = 31 * signature + (text?.toString()?.hashCode() ?: 0)
            }
            state.blocks.forEach { block ->
                when (block) {
                    is RichTextBlockEditState -> add(block.text.textFieldState.text)
                    is CodeBlockEditState -> add(block.code.text)
                    is TableBlockEditState ->
                        block.allCells.forEach { add(it.textFieldState.text) }

                    else -> add(null)
                }
            }
            signature
        }
            .drop(1)
            .collect {
                if (state.documentSelection != null) {
                    state.documentSelection = null
                    textToolbar.hide()
                }
            }
    }

    DisposableEffect(state, textToolbar) {
        onDispose {
            if (state.documentSelection != null) {
                state.documentSelection = null
                textToolbar.hide()
            }
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
        Box(modifier = modifier) {
            RichDocumentBlocks(
                state = state,
                visualStyles = visualStyles,
                colors = colors,
                focusRequesters = focusRequesters,
                geometry = geometry,
                textToolbar = textToolbar,
                clipboard = clipboard,
                haptics = haptics,
                scope = scope,
                listState = listState,
            )
            DocumentSelectionHandles(
                state = state,
                geometry = geometry,
                color = colors.text,
                onDragStart = { textToolbar.hide() },
                onDragStopped = {
                    showDocumentSelectionMenu(state, geometry, textToolbar, clipboard, scope)
                },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun RichDocumentBlocks(
    state: RichDocumentState,
    visualStyles: RichSpanVisualStyles,
    colors: MarkdownColors,
    focusRequesters: MutableMap<Int, FocusRequester>,
    geometry: RichSelectionGeometry,
    textToolbar: TextToolbar,
    clipboard: Clipboard,
    haptics: HapticFeedback,
    scope: CoroutineScope,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { geometry.editorCoordinates = it }
            .pointerInput(state, geometry) {
                detectDocumentSelection(
                    state = state,
                    geometry = geometry,
                    haptics = haptics,
                    onClearSelection = {
                        state.documentSelection = null
                        textToolbar.hide()
                    },
                    onSelectionFinished = {
                        showDocumentSelectionMenu(state, geometry, textToolbar, clipboard, scope)
                    },
                    // A word selection completed inside one field becomes a document selection
                    // on release, so the handles the user then grabs are the cross-block ones —
                    // the field's own popup handles can never leave their field. Deferred a
                    // frame so the field's own up-handling settles before it is retired.
                    onConvertSelection = { blockIndex, range ->
                        scope.launch {
                            withFrameNanos { }
                            state.documentSelection = RichDocSelection(
                                RichDocPosition(blockIndex, range.min),
                                RichDocPosition(blockIndex, range.max),
                            )
                            editableFieldStateAt(state, blockIndex)?.let { field ->
                                if (!field.selection.collapsed) {
                                    field.edit { selection = TextRange(field.selection.max) }
                                }
                            }
                            showDocumentSelectionMenu(
                                state, geometry, textToolbar, clipboard, scope,
                            )
                        }
                    },
                )
            }
            .testTag(RICH_DOCUMENT_EDITOR_TAG),
        // No top padding: matches the chunked source editor so switching edit modes does not
        // shift the content down (the collapsing top bar already provides the top spacing).
        // ime excluded: the editor container already applies imePadding.
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp + WindowInsets.navigationBars.exclude(WindowInsets.ime)
                .asPaddingValues().calculateBottomPadding(),
        ),
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
                    geometry = geometry,
                )

                is CodeBlockEditState ->
                    CodeBlockRow(index, block, state, visualStyles, colors, geometry)

                is ThematicBreakEditState -> HorizontalDivider(
                    color = colors.divider,
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                is TableBlockEditState ->
                    TableBlockRow(index, block, state, visualStyles, colors, geometry)

                is RawSourceEditState -> RawSourceRow(index, block, colors, geometry)
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
                            // Through pendingFocus so the caret predictably lands at the END
                            // of the last block, not wherever it last was.
                            val length = state.blockTextLength(lastText) ?: 0
                            state.pendingFocus =
                                RichFocusRequest(lastText, TextRange(length))
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
    geometry: RichSelectionGeometry,
) {
    val kind = block.kind
    DisposableEffect(geometry, index) {
        onDispose { geometry.clear(index) }
    }
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
        // The field is recreated when its TEXT STYLE changes (paragraph <-> heading, level
        // cycles): a live field does not re-layout on a style-only change, leaving text and
        // cursor at the old size. Focus survives via the pendingFocus request the retype
        // records. Other retypes keep the stable node, per the rule above.
        key((kind as? RichBlockKind.Heading)?.level ?: 0) {
            BasicTextField(
                state = block.text.textFieldState,
                textStyle = blockTextStyle(kind, colors),
                cursorBrush = SolidColor(colors.text),
                inputTransformation = remember(block, index) {
                    RichSpanInputTransformation(
                        state = block.text,
                        onSplit = { start, end -> state.requestSplit(index, start, end) },
                        documentSelectionActive = { state.documentSelection != null },
                        onReplaceDocumentSelection = { state.requestReplaceSelection(it) },
                        onSmartInsert = { start, end, text ->
                            state.requestSmartInsert(index, start, end, text)
                        },
                    )
                },
                outputTransformation = remember(block, visualStyles, index) {
                    RichSpanOutputTransformation(block.text, visualStyles) {
                        state.documentSelectionIn(index)
                    }
                },
                onTextLayout = { getResult -> geometry.textLayoutProviders[index] = getResult },
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned {
                        geometry.fieldBounds[index] = Rect(it.positionInRoot(), it.size.toSize())
                    }
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
                    .onFocusChanged {
                        if (it.isFocused) {
                            state.focusedIndex = index
                            state.focusedCell = null
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        when {
                            handleUndoRedoKey(event, state) -> true

                            event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.Backspace || event.key == Key.Delete) &&
                                    state.documentSelection != null -> {
                                state.requestDeleteSelection()
                                true
                            }

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
}

@Composable
private fun CodeBlockRow(
    index: Int,
    block: CodeBlockEditState,
    state: RichDocumentState,
    visualStyles: RichSpanVisualStyles,
    colors: MarkdownColors,
    geometry: RichSelectionGeometry,
) {
    DisposableEffect(geometry, index) {
        onDispose { geometry.clear(index) }
    }
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
            inputTransformation = remember(block) {
                DocumentSelectionInputTransformation(
                    documentSelectionActive = { state.documentSelection != null },
                    onReplaceDocumentSelection = { state.requestReplaceSelection(it) },
                )
            },
            outputTransformation = remember(block, visualStyles, index) {
                DocumentSelectionOutputTransformation(visualStyles) {
                    state.documentSelectionIn(index)
                }
            },
            onTextLayout = { getResult -> geometry.textLayoutProviders[index] = getResult },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    geometry.fieldBounds[index] = Rect(it.positionInRoot(), it.size.toSize())
                }
                .onFocusChanged {
                    if (it.isFocused) {
                        state.focusedIndex = index
                        state.focusedCell = null
                    }
                }
                .onPreviewKeyEvent { event ->
                    when {
                        handleUndoRedoKey(event, state) -> true

                        event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Backspace || event.key == Key.Delete) &&
                                state.documentSelection != null -> {
                            state.requestDeleteSelection()
                            true
                        }

                        else -> false
                    }
                }
                .testTag(richBlockFieldTag(index)),
        )
    }
}

@Composable
private fun TableBlockRow(
    index: Int,
    block: TableBlockEditState,
    state: RichDocumentState,
    visualStyles: RichSpanVisualStyles,
    colors: MarkdownColors,
    geometry: RichSelectionGeometry,
) {
    DisposableEffect(geometry, index) {
        onDispose { geometry.clear(index) }
    }
    // Cell TEXTS are editable in place; table structure is not. Chrome mirrors the preview's
    // table frame, and the row registers like a raw row so the cross-block selection gesture
    // stays out.
    Column(
        modifier = Modifier
            .padding(MarkdownBlockSpacing.block)
            .horizontalScroll(rememberScrollState())
            .border(1.dp, colors.divider, RoundedCornerShape(4.dp))
            .onGloballyPositioned {
                geometry.rawBounds[index] = Rect(it.positionInRoot(), it.size.toSize())
            },
    ) {
        TableEditRow(index, 0, block.header, header = true, state, visualStyles, colors)
        HorizontalDivider(color = colors.divider)
        block.rows.forEachIndexed { rowIndex, row ->
            TableEditRow(index, rowIndex + 1, row, header = false, state, visualStyles, colors)
            HorizontalDivider(color = colors.divider)
        }
    }
}

@Composable
private fun TableEditRow(
    blockIndex: Int,
    rowIndex: Int,
    cells: List<RichTextBlockState>,
    header: Boolean,
    state: RichDocumentState,
    visualStyles: RichSpanVisualStyles,
    colors: MarkdownColors,
) {
    Row {
        cells.forEachIndexed { columnIndex, cellState ->
            LaunchedEffect(cellState) {
                snapshotFlow { cellState.textFieldState.selection }
                    .collect { cellState.syncTypingStylesToCaret() }
            }
            BasicTextField(
                state = cellState.textFieldState,
                textStyle = MaterialTheme.typography.bodyMedium
                    .let { if (header) it.copy(fontWeight = FontWeight.Bold) else it }
                    .copy(color = colors.text),
                cursorBrush = SolidColor(colors.text),
                inputTransformation = remember(cellState) {
                    // Cells are single-line: the no-op split swallows Enter.
                    RichSpanInputTransformation(
                        state = cellState,
                        onSplit = { _, _ -> },
                        sanitizeNewlines = true,
                    )
                },
                outputTransformation = remember(cellState, visualStyles) {
                    RichSpanOutputTransformation(cellState, visualStyles)
                },
                modifier = Modifier
                    .width(TableEditCellWidth)
                    .padding(8.dp)
                    .onFocusChanged {
                        if (it.isFocused) {
                            state.focusedIndex = blockIndex
                            state.focusedCell = cellState
                        }
                    }
                    .onPreviewKeyEvent { handleUndoRedoKey(it, state) }
                    .testTag(richTableCellTag(blockIndex, rowIndex, columnIndex)),
            )
        }
    }
}

@Composable
private fun RawSourceRow(
    index: Int,
    block: RawSourceEditState,
    colors: MarkdownColors,
    geometry: RichSelectionGeometry,
) {
    DisposableEffect(geometry, index) {
        onDispose { geometry.clear(index) }
    }
    // Rendered through the preview's block components so unsupported constructs (tables,
    // HTML, images) look exactly like View mode. Not editable — the verbatim source slice
    // still serializes untouched, which is what keeps them lossless. The SelectionContainer
    // keeps this content copyable like every other block (the editable blocks get selection
    // from their text fields).
    val nodes = remember(block.source) { parseTopLevelBlocks(block.source) }
    SelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    geometry.rawBounds[index] = Rect(it.positionInRoot(), it.size.toSize())
                },
        ) {
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

private val TableEditCellWidth = 140.dp

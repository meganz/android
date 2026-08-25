package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import mega.privacy.android.feature.texteditor.components.MarkdownBlockSpacing
import mega.privacy.android.feature.texteditor.components.MarkdownCodeFrame
import mega.privacy.android.feature.texteditor.components.MarkdownColors
import mega.privacy.android.feature.texteditor.components.MarkdownListItemFrame
import mega.privacy.android.feature.texteditor.components.MarkdownListMarker
import mega.privacy.android.feature.texteditor.components.MarkdownQuoteFrame
import mega.privacy.android.feature.texteditor.components.markdownHeadingStyle
import mega.privacy.android.feature.texteditor.components.rememberMarkdownColors
import timber.log.Timber

const val RICH_DOCUMENT_EDITOR_TAG = "rich_document_editor"

fun richBlockFieldTag(index: Int): String = "rich_document_editor:block_$index"

const val RICH_DOCUMENT_TAIL_TAP_TAG = "rich_document_editor:tail_tap_area"

private val TailTapAreaHeight = 160.dp

/**
 * The rich editor surface: every block is always directly editable — no Markdown syntax, no
 * tap-to-reveal. Block structure (bullets, numbers, checkboxes, quote bars, code boxes) is
 * chrome drawn outside the text fields; inline formatting is applied by the span engine.
 */
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
    val field: @Composable RowScope.() -> Unit = {
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
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) state.focusedIndex = index }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key == Key.Backspace &&
                        block.text.textFieldState.selection == TextRange.Zero
                    ) {
                        state.mergeBlockBackward(index)
                    } else {
                        false
                    }
                }
                .testTag(richBlockFieldTag(index)),
        )
    }
    when (kind) {
        is RichBlockKind.Item -> MarkdownListItemFrame(
            marker = when {
                kind.checked != null -> MarkdownListMarker.TaskCheckbox(
                    checked = kind.checked,
                    onCheckedChange = { block.kind = kind.copy(checked = it) },
                )

                kind.ordered -> MarkdownListMarker.Number(orderedNumber(state.blocks, index))
                else -> MarkdownListMarker.Bullet
            },
            colors = colors,
            indent = kind.indent,
            content = field,
        )

        is RichBlockKind.Quote -> MarkdownQuoteFrame(
            depth = kind.depth,
            colors = colors,
            content = field,
        )

        RichBlockKind.Paragraph, is RichBlockKind.Heading -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    when (kind) {
                        is RichBlockKind.Heading -> MarkdownBlockSpacing.heading(kind.level)
                        else -> MarkdownBlockSpacing.block
                    },
                ),
            verticalAlignment = Alignment.Top,
            content = { field() },
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
                .testTag(richBlockFieldTag(index)),
        )
    }
}

@Composable
private fun RawSourceRow(block: RawSourceEditState, colors: MarkdownColors) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(colors.codeBackground, RoundedCornerShape(4.dp))
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
    ) {
        BasicText(
            text = block.source,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                color = colors.text,
            ),
        )
    }
}

@Composable
private fun blockTextStyle(kind: RichBlockKind, colors: MarkdownColors): TextStyle = when (kind) {
    is RichBlockKind.Heading -> markdownHeadingStyle(kind.level).copy(color = colors.text)
    else -> MaterialTheme.typography.bodyMedium.copy(color = colors.text)
}

/** 1-based number of an ordered item: counts ordered predecessors at the same indent. */
private fun orderedNumber(blocks: List<RichBlockEditState>, index: Int): Int {
    val kind = (blocks.getOrNull(index) as? RichTextBlockEditState)?.kind
            as? RichBlockKind.Item ?: return 1
    var number = 1
    for (i in index - 1 downTo 0) {
        val previous = (blocks.getOrNull(i) as? RichTextBlockEditState)?.kind
                as? RichBlockKind.Item ?: break
        when {
            previous.indent < kind.indent -> break
            previous.indent > kind.indent -> continue
            previous.ordered -> number++
            else -> break
        }
    }
    return number
}

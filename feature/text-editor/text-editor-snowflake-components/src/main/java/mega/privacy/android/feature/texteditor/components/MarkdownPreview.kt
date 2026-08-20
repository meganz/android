package mega.privacy.android.feature.texteditor.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import mega.android.core.ui.components.indicators.LargeInfiniteSpinnerIndicator
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Node
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser

/**
 * Cross-block text selection requires one [SelectionContainer] around the whole document, which is
 * only safe without item recycling, so it is limited to documents at most this many characters
 * (composed eagerly in a plain [Column]). Sized to the chunked editor's per-chunk cap: eagerly
 * composing one chunk's worth of text is known to be safe (AND-23707). Both caps verified on a
 * Pixel 7; the feature is flag-gated, so lower these first if low-end devices report jank.
 */
private const val FULL_SELECTION_MAX_CHARS = 50_000

/**
 * Companion guard to [FULL_SELECTION_MAX_CHARS] for pathological many-block files (thousands of
 * one-word lines stay under the char cap but would still compose thousands of nodes eagerly).
 */
private const val FULL_SELECTION_MAX_BLOCKS = 500

/**
 * In-house read-only Markdown preview: parses with CommonMark (+ GFM tables) on a background
 * dispatcher (so multi-MB documents don't ANR), then renders each top-level block with our own
 * Compose composables ([MarkdownBlock]). No third-party UI dependency, no size cap.
 *
 * [lazyListState] is hoisted by the caller so the scroll position survives leaving and
 * re-entering the preview (e.g. Preview -> Edit -> Preview).
 *
 * Text selection has two paths. Documents within [FULL_SELECTION_MAX_CHARS] /
 * [FULL_SELECTION_MAX_BLOCKS] render in a plain [Column] under a single [SelectionContainer]
 * ([FullSelectionPreview]), so a selection can span blocks and Select All covers the whole
 * document. Larger documents render in a virtualized [LazyColumn] where each item hosts its own
 * container, limiting a selection to one block — a single container around a [LazyColumn] crashes
 * because selectables in recycled items can't be resolved (JetBrains/compose-multiplatform#1280),
 * which is why the chunked source view (TextEditorContent) uses the same per-item structure.
 */
@Composable
fun MarkdownPreview(
    content: String,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    restoreLine: Int? = null,
    onRestoreConsumed: () -> Unit = {},
    onTopLine: (Int) -> Unit = {},
) {
    val colors = rememberMarkdownColors()
    val selectionColors = remember(colors.text) {
        TextSelectionColors(
            handleColor = colors.text,
            backgroundColor = colors.text.copy(alpha = 0.3f),
        )
    }
    // A SelectionContainer only clears its selection on a tap within its own bounds, and has no
    // programmatic clear, so a tap on the empty space around the text never drops it. Bumping this
    // key recreates the containers, which does.
    var selectionResetKey by remember { mutableIntStateOf(0) }
    // A plain map on purpose — an idempotent cache, never observed for recomposition; see
    // LocalMarkdownScrollStates.
    val blockScrollStates = remember(content) { mutableMapOf<Node, ScrollState>() }
    val blocks by produceState<List<Node>?>(initialValue = null, content) {
        value = withContext(Dispatchers.Default) { parseTopLevelBlocks(content) }
    }
    val parsed = blocks
    val fullSelection = parsed != null && parsed.isNotEmpty() &&
            content.length <= FULL_SELECTION_MAX_CHARS && parsed.size <= FULL_SELECTION_MAX_BLOCKS

    if (!fullSelection) {
        // Report the top visible source line (absolute) so the chunked view can land on the same
        // line when switching to Edit — one shared, precise position metric. The first visible
        // block is usually partially scrolled off the top, so interpolate within it by how far it
        // is scrolled.
        LaunchedEffect(lazyListState, parsed) {
            val items = parsed ?: return@LaunchedEffect
            if (items.isEmpty()) return@LaunchedEffect
            snapshotFlow {
                val first = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()
                Triple(first?.index ?: 0, (-(first?.offset ?: 0)).coerceAtLeast(0), first?.size ?: 0)
            }.collect { (index, scrolledOffPx, sizePx) ->
                val startLine = items.getOrNull(index)?.startLine() ?: 0
                val nextStartLine = items.getOrNull(index + 1)?.startLine() ?: (startLine + 1)
                val fraction =
                    if (sizePx > 0) (scrolledOffPx.toFloat() / sizePx).coerceIn(0f, 1f) else 0f
                onTopLine(startLine + (fraction * (nextStartLine - startLine)).toInt())
            }
        }

        // Restore to a saved line (from Edit or Continue-Where-Left-Off): scroll to the block that
        // CONTAINS the target line — the last block starting at or before it. Using the first
        // block at/after the line would overshoot when the line falls inside a multi-line block.
        LaunchedEffect(parsed, restoreLine) {
            val items = parsed ?: return@LaunchedEffect
            val targetLine = restoreLine ?: return@LaunchedEffect
            if (items.isNotEmpty()) {
                val index = items.indexOfLast { it.startLine() <= targetLine }.coerceAtLeast(0)
                lazyListState.scrollToItem(index)
            }
            onRestoreConsumed()
        }
    }
    CompositionLocalProvider(
        LocalTextSelectionColors provides selectionColors,
        LocalMarkdownScrollStates provides blockScrollStates,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        // Non-consuming: only a clean tap — one not claimed by text selection, a
                        // link, the scrollbar, or list scrolling — clears the selection, and the
                        // event stays available to the screen's tap-to-reveal-bars handler.
                        if (waitForUpOrCancellation() != null) selectionResetKey++
                    }
                },
        ) {
            when {
                parsed == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LargeInfiniteSpinnerIndicator()
                }

                fullSelection -> FullSelectionPreview(
                    blocks = parsed,
                    colors = colors,
                    selectionResetKey = selectionResetKey,
                    restoreLine = restoreLine,
                    onRestoreConsumed = onRestoreConsumed,
                    onTopLine = onTopLine,
                )

                else -> {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        items(parsed.size) { index ->
                            key(selectionResetKey) {
                                SelectionContainer {
                                    MarkdownBlock(node = parsed[index], colors = colors)
                                }
                            }
                        }
                    }
                    MarkdownScrollbar(
                        state = lazyListState,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
        }
    }
}

/**
 * Small-document render path: every block composed eagerly in one scrollable [Column] inside a
 * single [SelectionContainer], so a selection spans blocks and Select All covers the document.
 * Block top offsets are measured into [blockTops] to map the scroll offset to a source line (and
 * back for [restoreLine]) — the same line metric the [LazyColumn] path derives from item geometry.
 */
@Composable
private fun FullSelectionPreview(
    blocks: List<Node>,
    colors: MarkdownColors,
    selectionResetKey: Int,
    restoreLine: Int?,
    onRestoreConsumed: () -> Unit,
    onTopLine: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Both hoisted above key() below so clearing a selection keeps the scroll position. Unlike
    // the horizontal-scroll cache, blockTops IS snapshot state: the restore effect below awaits
    // its population through snapshotFlow.
    val scrollState = rememberScrollState()
    val blockTops = remember(blocks) { mutableStateMapOf<Int, Int>() }

    LaunchedEffect(scrollState, blocks) {
        snapshotFlow { scrollState.value }.collect { value ->
            val index = blocks.indices.lastOrNull { (blockTops[it] ?: Int.MAX_VALUE) <= value } ?: 0
            val top = blockTops[index] ?: 0
            val nextTop = blockTops[index + 1]
            val startLine = blocks[index].startLine()
            val nextStartLine = blocks.getOrNull(index + 1)?.startLine() ?: (startLine + 1)
            val fraction = if (nextTop != null && nextTop > top) {
                ((value - top).toFloat() / (nextTop - top)).coerceIn(0f, 1f)
            } else {
                0f
            }
            onTopLine(startLine + (fraction * (nextStartLine - startLine)).toInt())
        }
    }

    LaunchedEffect(blocks, restoreLine) {
        val targetLine = restoreLine ?: return@LaunchedEffect
        val index = blocks.indexOfLast { it.startLine() <= targetLine }.coerceAtLeast(0)
        val top = snapshotFlow { blockTops[index] }.filterNotNull().first()
        scrollState.scrollTo(top)
        onRestoreConsumed()
    }

    Box(modifier = modifier.fillMaxSize()) {
        key(selectionResetKey) {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                ) {
                    blocks.forEachIndexed { index, node ->
                        MarkdownBlock(
                            node = node,
                            colors = colors,
                            modifier = Modifier.onGloballyPositioned {
                                blockTops[index] = it.positionInParent().y.roundToInt()
                            },
                        )
                    }
                }
            }
        }
        MarkdownColumnScrollbar(
            state = scrollState,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

private val markdownParser: Parser by lazy {
    Parser.builder()
        .extensions(listOf(TablesExtension.create()))
        .includeSourceSpans(IncludeSourceSpans.BLOCKS)
        .build()
}

/** 0-based source line index of a block's first source span (0 if unavailable). */
private fun Node.startLine(): Int = sourceSpans.firstOrNull()?.lineIndex ?: 0

/** Parses [content] and returns its top-level block nodes (document children). */
private fun parseTopLevelBlocks(content: String): List<Node> {
    val document = markdownParser.parse(content)
    return buildList {
        var node = document.firstChild
        while (node != null) {
            add(node)
            node = node.next
        }
    }
}

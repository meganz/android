package mega.privacy.android.feature.texteditor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.android.core.ui.components.indicators.LargeInfiniteSpinnerIndicator
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Node
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser

/**
 * In-house read-only Markdown preview: parses with CommonMark (+ GFM tables) on a background
 * dispatcher (so multi-MB documents don't ANR), then renders each top-level block with our own
 * Compose composables ([MarkdownBlock]) in a virtualized [LazyColumn]. [MarkdownScrollbar] tracks
 * the scroll position from measured block heights. No third-party UI dependency, no size cap.
 *
 * [lazyListState] is hoisted by the caller so the scroll position survives leaving and
 * re-entering the preview (e.g. Preview -> Edit -> Preview).
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
    val blocks by produceState<List<Node>?>(initialValue = null, content) {
        value = withContext(Dispatchers.Default) { parseTopLevelBlocks(content) }
    }
    val parsed = blocks

    // Report the top visible source line (absolute) so the chunked view can land on the same line
    // when switching to Edit — one shared, precise position metric. The first visible block is
    // usually partially scrolled off the top, so interpolate within it by how far it is scrolled.
    LaunchedEffect(lazyListState, parsed) {
        val items = parsed ?: return@LaunchedEffect
        if (items.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val first = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()
            Triple(first?.index ?: 0, (-(first?.offset ?: 0)).coerceAtLeast(0), first?.size ?: 0)
        }.collect { (index, scrolledOffPx, sizePx) ->
            val startLine = items.getOrNull(index)?.startLine() ?: 0
            val nextStartLine = items.getOrNull(index + 1)?.startLine() ?: (startLine + 1)
            val fraction = if (sizePx > 0) (scrolledOffPx.toFloat() / sizePx).coerceIn(0f, 1f) else 0f
            onTopLine(startLine + (fraction * (nextStartLine - startLine)).toInt())
        }
    }

    // Restore to a saved line (from Edit or Continue-Where-Left-Off): scroll to the block that
    // CONTAINS the target line — the last block starting at or before it. Using the first block
    // at/after the line would overshoot when the line falls inside a multi-line block.
    LaunchedEffect(parsed, restoreLine) {
        val items = parsed ?: return@LaunchedEffect
        val targetLine = restoreLine ?: return@LaunchedEffect
        if (items.isNotEmpty()) {
            val index = items.indexOfLast { it.startLine() <= targetLine }.coerceAtLeast(0)
            lazyListState.scrollToItem(index)
        }
        onRestoreConsumed()
    }
    Box(modifier = modifier.fillMaxSize()) {
        if (parsed == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LargeInfiniteSpinnerIndicator()
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(parsed.size) { index ->
                    MarkdownBlock(node = parsed[index], colors = colors)
                }
            }
            MarkdownScrollbar(
                state = lazyListState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
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

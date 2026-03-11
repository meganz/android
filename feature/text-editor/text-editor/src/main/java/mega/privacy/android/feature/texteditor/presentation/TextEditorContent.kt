package mega.privacy.android.feature.texteditor.presentation

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Line height used for both line numbers and text field so the number column aligns with content. */
private val EditorLineHeight = 20.sp

/**
 * Line number gutter: fixed width so the editor text never shifts. When 4 digits are needed (1000+ lines),
 * we shrink the line number text size so they still fit in the same gutter.
 * [LineNumberGutterPadding] = gap between numbers and editor text (legacy line_number_padding 6dp).
 */
private val LineNumberGutterWidth = 32.dp
private val LineNumberGutterPadding = 6.dp

/** Line number text size for 1–3 digits. */
private val LineNumberTextSize = 12.sp

/** Smaller size for 4 digits (1000+ lines) so they fit in the fixed gutter without layout shift. */
private val LineNumberTextSize4Digits = 10.sp

/** Max visible lines to compute per frame; avoids main-thread work when viewport is unknown or range is huge. */
private const val MaxVisibleLineNumbers = 512

@Composable
internal fun TextEditorContent(
    content: String,
    showLineNumbers: Boolean,
    readOnly: Boolean,
    onContentChange: (String) -> Unit,
    hasMoreLines: Boolean = false,
    onNearEndOfScroll: (() -> Unit)? = null,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scrollState = rememberScrollState()
    val textStyle = editorTextStyle(MaterialTheme.colorScheme.onSurface)
    val density = LocalDensity.current

    var lineStartOffsets by remember(content) { mutableStateOf<List<Int>?>(null) }
    LaunchedEffect(content, defaultDispatcher) {
        lineStartOffsets = computeLineStartOffsets(content, defaultDispatcher)
    }

    val maxLineNumber = remember(lineStartOffsets) {
        (lineStartOffsets?.size ?: 1).coerceAtLeast(1)
    }
    val lineNumberDigitCount = digitCountForMaxLine(maxLineNumber)

    var viewportHeightPx by remember { mutableStateOf(0f) }
    val lineHeightPx = with(density) { EditorLineHeight.toPx() }

    // Scroll restoration: when load-more appends content, recomposition can reset scroll.
    // We save the scroll position when triggering load-more and restore it after content updates,
    // so the list does not jump. Cleared when the user scrolls away from the bottom.
    var scrollPositionToRestorePx by remember { mutableStateOf<Int?>(null) }
    var lastLoadMoreAtMaxValue by remember { mutableStateOf(0) }

    LaunchedEffect(hasMoreLines, onNearEndOfScroll, viewportHeightPx) {
        if (!hasMoreLines || onNearEndOfScroll == null || viewportHeightPx <= 0f) return@LaunchedEffect
        val threshold = (viewportHeightPx * 5f).coerceAtLeast(100f)
        snapshotFlow { scrollState.value to scrollState.maxValue }
            .collect { (value, maxValue) ->
                if (maxValue > 0 && (maxValue - value) < threshold) {
                    if (maxValue > lastLoadMoreAtMaxValue) {
                        scrollPositionToRestorePx = scrollState.value
                        onNearEndOfScroll()
                        lastLoadMoreAtMaxValue = maxValue
                    }
                } else {
                    scrollPositionToRestorePx = null
                    lastLoadMoreAtMaxValue = 0
                }
            }
    }

    // After content grows from load-more, restore scroll so the user stays at the same place.
    LaunchedEffect(content) {
        scrollPositionToRestorePx?.let { scrollPx ->
            scrollState.scrollTo(scrollPx)
            scrollPositionToRestorePx = null
        }
    }

    val visibleLinesToShow by remember(
        textLayoutResult,
        content,
        lineStartOffsets,
        viewportHeightPx,
    ) {
        derivedStateOf {
            val layout = textLayoutResult ?: return@derivedStateOf emptyList<Pair<Int, String>>()
            val offsets = lineStartOffsets ?: return@derivedStateOf emptyList()
            computeVisibleLineNumbers(
                layout = layout,
                content = content,
                lineStartOffsets = offsets,
                scrollPx = scrollState.value,
                viewportHeightPx = viewportHeightPx,
                lineHeightPx = lineHeightPx,
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth > 0.dp && maxHeight > 0.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (showLineNumbers) 0.dp else 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    )
                    .onSizeChanged { viewportHeightPx = it.height.toFloat() }
                    .verticalScroll(scrollState),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    if (showLineNumbers) {
                        val lineNumberColor = MaterialTheme.colorScheme.onSurface
                        val lineNumSp =
                            if (lineNumberDigitCount >= 4) LineNumberTextSize4Digits else LineNumberTextSize
                        val lineNumTextSizePx = with(density) { lineNumSp.toPx() }
                        val paint = remember(lineNumTextSizePx, lineNumberColor) {
                            Paint().apply {
                                isAntiAlias = true
                                textSize = lineNumTextSizePx
                                color = lineNumberColor.toArgb()
                            }
                        }
                        Canvas(
                            modifier = Modifier
                                .width(LineNumberGutterWidth)
                                .fillMaxHeight()
                                .padding(end = LineNumberGutterPadding),
                            onDraw = drawScope@{
                                val layout = textLayoutResult ?: return@drawScope
                                val gutterWidthPx = size.width
                                for ((visualIndex, lineNum) in visibleLinesToShow) {
                                    if (lineNum.isEmpty() || visualIndex >= layout.lineCount) continue
                                    val textWidth = paint.measureText(lineNum)
                                    val x = gutterWidthPx - textWidth
                                    val y = layout.getLineBaseline(visualIndex)
                                    drawContext.canvas.nativeCanvas.drawText(lineNum, x, y, paint)
                                }
                            },
                        )
                    }
                    BasicTextField(
                        value = content,
                        onValueChange = onContentChange,
                        readOnly = readOnly,
                        textStyle = textStyle,
                        modifier = if (showLineNumbers) Modifier
                            .weight(1f)
                            .fillMaxWidth()
                        else Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        maxLines = Int.MAX_VALUE,
                        onTextLayout = { textLayoutResult = it },
                    )
                }
            }
        }
    }
}

private fun digitCountForMaxLine(maxLineNumber: Int): Int = when {
    maxLineNumber < 10 -> 1
    maxLineNumber < 100 -> 2
    maxLineNumber < 1000 -> 3
    else -> 4
}

/**
 * Shared text style for gutter and editor. Uses [PlatformTextStyle] with [includeFontPadding][PlatformTextStyle.includeFontPadding] = false
 * so that line numbers (digits only) and editor text (mixed characters) align vertically; without this, Android's default
 * font padding can differ between the two and cause drift (see "External Synchronized Gutter" in line-numbering research).
 */
private fun editorTextStyle(color: Color): TextStyle = TextStyle(
    color = color,
    fontSize = 14.sp,
    lineHeight = EditorLineHeight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/**
 * Returns the logical line number to show for visual line [visualLineIndex], or null if this visual line
 * is a continuation of a wrapped logical line (IDE-style: number only at start of each logical line).
 * Uses [TextLayoutResult.getLineStart]: if the character before this line's start is '\n', it's a new logical line.
 * [lineStartOffsets] must be the precomputed list of character offsets where each logical line starts (for O(log n) lookup).
 */
private fun logicalLineNumberForVisualLine(
    layout: TextLayoutResult,
    content: String,
    lineStartOffsets: List<Int>,
    visualLineIndex: Int,
): Int? {
    if (visualLineIndex == 0) return 1
    val lineStart = layout.getLineStart(visualLineIndex)
    if (lineStart <= 0) return 1
    if (content.getOrNull(lineStart - 1) != '\n') return null
    val idx = lineStartOffsets.binarySearch(lineStart)
    return if (idx >= 0) idx + 1 else -idx
}

/**
 * Precomputes the character offset of each logical line start (after each '\n').
 * Used for O(log n) line number lookup. Runs on [dispatcher] (CPU-bound work) so the main thread is not blocked.
 * Empty content returns [0].
 */
private suspend fun computeLineStartOffsets(
    content: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): List<Int> =
    withContext(dispatcher) {
        if (content.isEmpty()) return@withContext listOf(0)
        buildList {
            add(0)
            for (i in content.indices) {
                if (content[i] == '\n') add(i + 1)
            }
        }
    }

/**
 * Visible viewport line range (capped for performance); then resolves logical line numbers per visual line.
 * Pure function for testability; used inside [derivedStateOf] in [TextEditorContent].
 */
private fun computeVisibleLineNumbers(
    layout: TextLayoutResult,
    content: String,
    lineStartOffsets: List<Int>,
    scrollPx: Int,
    viewportHeightPx: Float,
    lineHeightPx: Float,
): List<Pair<Int, String>> {
    val lineCount = layout.lineCount.coerceAtLeast(1)
    val hasValidViewport = viewportHeightPx > 0 && lineHeightPx > 0

    val visibleRange = if (hasValidViewport) {
        val first = (scrollPx / lineHeightPx).toInt().coerceIn(0, lineCount - 1)
        val overscan = 2
        val lastUncapped = (((scrollPx + viewportHeightPx) / lineHeightPx).toInt() + overscan)
            .coerceIn(first, lineCount - 1)
        val last = minOf(
            lastUncapped,
            first + MaxVisibleLineNumbers - 1,
            lineCount - 1,
        )
        first..last
    } else {
        0..minOf(MaxVisibleLineNumbers - 1, lineCount - 1)
    }

    return visibleRange.mapNotNull { visualIndex ->
        logicalLineNumberForVisualLine(layout, content, lineStartOffsets, visualIndex)
            ?.let { num -> visualIndex to num.toString() }
    }
}

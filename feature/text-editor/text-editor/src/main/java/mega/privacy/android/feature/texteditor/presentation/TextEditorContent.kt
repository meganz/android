package mega.privacy.android.feature.texteditor.presentation

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Line height used for both line numbers and text field so the number column aligns with content. */
internal val EditorLineHeight = 20.sp

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

@Suppress("DEPRECATION") // LocalAutofill: only way to prevent Compose from notifying platform autofill with large payload
@Composable
internal fun TextEditorContent(
    textFieldState: TextFieldState,
    scrollState: ScrollState,
    showLineNumbers: Boolean,
    readOnly: Boolean,
    appendSuffix: String? = null,
    onAppendSuffixConsumed: (() -> Unit)? = null,
    hasMoreLines: Boolean = false,
    onNearEndOfScroll: (() -> Unit)? = null,
) {
    val currentText = textFieldState.text.toString()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textStyle = editorTextStyle(MaterialTheme.colorScheme.onSurface)
    val density = LocalDensity.current
    var viewportHeightPx by remember { mutableStateOf(0f) }
    val lineHeightPx = with(density) { EditorLineHeight.toPx() }

    // When appending load-more suffix, set cursor to visible viewport so BasicTextField's bringIntoView doesn't scroll away (Pre-Phase 5.3).
    LaunchedEffect(appendSuffix) {
        if (appendSuffix != null) {
            val layout = textLayoutResult
            val midViewportPx = scrollState.value + (viewportHeightPx / 2f)
            val visibleCharOffset = layout
                ?.getLineForVerticalPosition(midViewportPx)
                ?.let { line -> layout.getLineStart(line) }

            textFieldState.edit {
                append(appendSuffix)
                val cursorPos = visibleCharOffset ?: length
                selection = TextRange(cursorPos)
            }
            onAppendSuffixConsumed?.invoke()
        }
    }

    // Keep previous offsets while new ones are computed to avoid a one-frame empty gutter flicker.
    var lineStartOffsets by remember { mutableStateOf(listOf(0)) }
    var lineStartOffsetsText by remember { mutableStateOf(currentText) }
    LaunchedEffect(currentText) {
        val computedOffsets = computeLineStartOffsets(currentText)
        lineStartOffsets = computedOffsets
        lineStartOffsetsText = currentText
    }

    val maxLineNumber = remember(lineStartOffsets) {
        lineStartOffsets.size.coerceAtLeast(1)
    }
    val lineNumberDigitCount = digitCountForMaxLine(maxLineNumber)

    // Scroll restoration: when load-more appends content, recomposition can reset scroll.
    var scrollPositionToRestoreForLoadMorePx by remember { mutableStateOf<Int?>(null) }
    var lastLoadMoreAtMaxValue by remember { mutableStateOf(0) }

    LaunchedEffect(hasMoreLines, onNearEndOfScroll, viewportHeightPx) {
        if (!hasMoreLines || onNearEndOfScroll == null || viewportHeightPx <= 0f) return@LaunchedEffect
        val threshold = (viewportHeightPx * 5f).coerceAtLeast(100f)
        snapshotFlow { scrollState.value to scrollState.maxValue }
            .collect { (value, maxValue) ->
                if (maxValue > 0 && (maxValue - value) < threshold) {
                    if (maxValue > lastLoadMoreAtMaxValue) {
                        scrollPositionToRestoreForLoadMorePx = scrollState.value
                        onNearEndOfScroll()
                        lastLoadMoreAtMaxValue = maxValue
                    }
                } else {
                    scrollPositionToRestoreForLoadMorePx = null
                    lastLoadMoreAtMaxValue = 0
                }
            }
    }

    // After content grows from load-more, restore scroll so the user stays at the same place.
    LaunchedEffect(currentText) {
        scrollPositionToRestoreForLoadMorePx?.let { scrollPx ->
            scrollState.scrollTo(scrollPx)
            scrollPositionToRestoreForLoadMorePx = null
        }
    }

    var stableVisibleLinesToShow by remember { mutableStateOf(listOf(0 to "1")) }
    val visibleLinesToShowCandidate by remember(
        textLayoutResult,
        currentText,
        lineStartOffsets,
        scrollState.value,
        viewportHeightPx,
    ) {
        derivedStateOf {
            val layout = textLayoutResult ?: return@derivedStateOf emptyList<Pair<Int, String>>()
            // During fast edits, text/layout/offsets can be out of sync for one frame.
            val layoutText = layout.layoutInput.text.text
            if (layoutText != currentText || layoutText != lineStartOffsetsText) {
                return@derivedStateOf stableVisibleLinesToShow
            }
            computeVisibleLineNumbers(
                layout = layout,
                content = currentText,
                lineStartOffsets = lineStartOffsets,
                scrollPx = scrollState.value,
                viewportHeightPx = viewportHeightPx,
                lineHeightPx = lineHeightPx,
            )
        }
    }

    LaunchedEffect(visibleLinesToShowCandidate) {
        if (visibleLinesToShowCandidate.isNotEmpty()) {
            stableVisibleLinesToShow = visibleLinesToShowCandidate
        }
    }

    CompositionLocalProvider(LocalAutofill provides null) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth > 0.dp && maxHeight > 0.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
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
                            val lineNumberPaint = remember(lineNumberDigitCount, lineNumberColor) {
                                Paint().apply {
                                    isAntiAlias = true
                                    textSize = with(density) {
                                        (if (lineNumberDigitCount >= 4) LineNumberTextSize4Digits else LineNumberTextSize).toPx()
                                    }
                                    color = lineNumberColor.toArgb()
                                }
                            }
                            Canvas(
                                modifier = Modifier
                                    .width(LineNumberGutterWidth)
                                    .fillMaxHeight()
                                    .padding(end = LineNumberGutterPadding)
                                    .clearAndSetSemantics { },
                                onDraw = drawScope@{
                                    val layout = textLayoutResult ?: return@drawScope
                                    val gutterWidthPx = size.width
                                    for ((visualIndex, lineNum) in stableVisibleLinesToShow) {
                                        if (lineNum.isEmpty() || visualIndex >= layout.lineCount) continue
                                        val textWidth = lineNumberPaint.measureText(lineNum)
                                        val x = gutterWidthPx - textWidth
                                        val y = layout.getLineBaseline(visualIndex)
                                        drawContext.canvas.nativeCanvas.drawText(lineNum, x, y, lineNumberPaint)
                                    }
                                },
                            )
                        }
                        BasicTextField(
                            state = textFieldState,
                            readOnly = readOnly,
                            textStyle = textStyle,
                            modifier = (if (showLineNumbers) Modifier
                                .weight(1f)
                                .fillMaxWidth()
                            else Modifier
                                .fillMaxWidth()
                                .wrapContentHeight())
                                .clearAndSetSemantics { },
                            lineLimits = TextFieldLineLimits.MultiLine(),
                            onTextLayout = { getResult -> textLayoutResult = getResult() },
                        )
                    }
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
    return if (idx >= 0) {
        idx + 1
    } else {
        // binarySearch returns (-insertionPoint - 1) when not found.
        // Logical line number should map to the previous logical line start (insertionPoint), not insertionPoint + 1.
        -(idx + 1)
    }
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

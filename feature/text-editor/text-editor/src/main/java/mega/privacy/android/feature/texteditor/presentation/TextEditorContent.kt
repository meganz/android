package mega.privacy.android.feature.texteditor.presentation

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Line height used for both line numbers and text field so the gutter aligns with content. */
internal val EditorLineHeight = 20.sp

private val LineNumberGutterWidth = 36.dp
private val LineNumberGutterPadding = 6.dp
private val LineNumberTextSize = 12.sp
private val LineNumberTextSizeSmall = 10.sp

/**
 * Virtualised text editor content backed by a [LazyColumn].
 *
 * - **View mode** — all chunks are [BasicText].
 * - **Edit mode** — only the focused chunk ±1 are [BasicTextField] (3 max).
 *   All other chunks are [BasicText] with a tap handler that shifts focus.
 *   This keeps memory and layout cost low regardless of file size.
 */
@Suppress("DEPRECATION")
@Composable
internal fun TextEditorContent(
    lazyListState: LazyListState,
    chunkCount: Int,
    totalLineCount: Int,
    chunkTextProvider: (chunkIndex: Int) -> String,
    chunkStateProvider: ((chunkIndex: Int) -> TextFieldState)?,
    chunkStartLineProvider: (chunkIndex: Int) -> Int,
    onChunkDisposed: ((chunkIndex: Int) -> Unit)?,
    isChunkReadOnly: (chunkIndex: Int) -> Boolean,
    onChunkFocused: ((chunkIndex: Int) -> Unit)?,
    showLineNumbers: Boolean,
    readOnly: Boolean,
) {
    val textStyle = editorTextStyle(MaterialTheme.colorScheme.onSurface)

    CompositionLocalProvider(LocalAutofill provides null) {
        if (readOnly) {
            ViewModeLazyColumn(
                lazyListState = lazyListState,
                chunkCount = chunkCount,
                totalLineCount = totalLineCount,
                chunkTextProvider = chunkTextProvider,
                chunkStartLineProvider = chunkStartLineProvider,
                showLineNumbers = showLineNumbers,
                textStyle = textStyle,
            )
        } else {
            EditModeLazyColumn(
                lazyListState = lazyListState,
                chunkCount = chunkCount,
                totalLineCount = totalLineCount,
                chunkStateProvider = checkNotNull(chunkStateProvider) {
                    "chunkStateProvider is required when readOnly is false"
                },
                chunkStartLineProvider = chunkStartLineProvider,
                onChunkDisposed = checkNotNull(onChunkDisposed) {
                    "onChunkDisposed is required when readOnly is false"
                },
                isChunkReadOnly = isChunkReadOnly,
                onChunkFocused = checkNotNull(onChunkFocused) {
                    "onChunkFocused is required when readOnly is false"
                },
                showLineNumbers = showLineNumbers,
                textStyle = textStyle,
            )
        }
    }
}

@Composable
private fun ViewModeLazyColumn(
    lazyListState: LazyListState,
    chunkCount: Int,
    totalLineCount: Int,
    chunkTextProvider: (Int) -> String,
    chunkStartLineProvider: (Int) -> Int,
    showLineNumbers: Boolean,
    textStyle: TextStyle,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(
                start = if (showLineNumbers) 0.dp else 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
    ) {
        items(
            count = chunkCount,
            key = { "chunk-$it" },
            contentType = { "readOnlyChunk" },
        ) { idx ->
            ReadOnlyChunkItem(
                chunkText = chunkTextProvider(idx),
                startLineNumber = chunkStartLineProvider(idx),
                maxLineNumber = totalLineCount,
                showLineNumbers = showLineNumbers,
                textStyle = textStyle,
            )
        }
    }
}

@Composable
private fun EditModeLazyColumn(
    lazyListState: LazyListState,
    chunkCount: Int,
    totalLineCount: Int,
    chunkStateProvider: (Int) -> TextFieldState,
    chunkStartLineProvider: (Int) -> Int,
    onChunkDisposed: (Int) -> Unit,
    isChunkReadOnly: (Int) -> Boolean,
    onChunkFocused: (Int) -> Unit,
    showLineNumbers: Boolean,
    textStyle: TextStyle,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(
                start = if (showLineNumbers) 0.dp else 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
    ) {
        items(
            count = chunkCount,
            key = { "chunk-$it" },
            contentType = { "editableChunk" },
        ) { idx ->
            val chunkState = chunkStateProvider(idx)
            DisposableEffect(idx) {
                onDispose { onChunkDisposed(idx) }
            }
            EditableChunkItem(
                textFieldState = chunkState,
                readOnly = isChunkReadOnly(idx),
                onFocused = { onChunkFocused(idx) },
                startLineNumber = chunkStartLineProvider(idx),
                maxLineNumber = totalLineCount,
                showLineNumbers = showLineNumbers,
                textStyle = textStyle,
            )
        }
    }
}

@Composable
private fun ReadOnlyChunkItem(
    chunkText: String,
    startLineNumber: Int,
    maxLineNumber: Int,
    showLineNumbers: Boolean,
    textStyle: TextStyle,
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    EditorChunkLayout(
        showLineNumbers = showLineNumbers,
        gutter = {
            LineNumberGutter(
                textLayoutResult = layoutResult,
                text = chunkText,
                startLineNumber = startLineNumber,
                maxLineNumber = maxLineNumber,
            )
        },
    ) {
        BasicText(
            text = chunkText,
            style = textStyle,
            onTextLayout = { layoutResult = it },
        )
    }
}

@Composable
private fun EditableChunkItem(
    textFieldState: TextFieldState,
    readOnly: Boolean,
    onFocused: () -> Unit,
    startLineNumber: Int,
    maxLineNumber: Int,
    showLineNumbers: Boolean,
    textStyle: TextStyle,
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    EditorChunkLayout(
        showLineNumbers = showLineNumbers,
        gutter = {
            LineNumberGutter(
                textLayoutResult = layoutResult,
                text = textFieldState.text.toString(),
                startLineNumber = startLineNumber,
                maxLineNumber = maxLineNumber,
            )
        },
    ) {
        BasicTextField(
            state = textFieldState,
            readOnly = readOnly,
            textStyle = textStyle,
            modifier = Modifier
                .onFocusChanged { if (it.isFocused) onFocused() }
                .clearAndSetSemantics { },
            lineLimits = TextFieldLineLimits.MultiLine(),
            onTextLayout = { getResult -> layoutResult = getResult() },
        )
    }
}

/**
 * Custom layout that measures text first, then constrains the gutter to the
 * same height. Replaces `Row(Modifier.height(IntrinsicSize.Min))` to avoid
 * the expensive double-measurement pass on large text blocks.
 */
@Composable
private fun EditorChunkLayout(
    showLineNumbers: Boolean,
    gutter: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    Layout(
        content = {
            content()
            if (showLineNumbers) gutter()
        },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val hasGutter = measurables.size > 1
        val gutterWidthPx = if (hasGutter) with(density) { LineNumberGutterWidth.roundToPx() } else 0

        val textPlaceable = measurables[0].measure(
            constraints.copy(
                minWidth = 0,
                maxWidth = (constraints.maxWidth - gutterWidthPx).coerceAtLeast(0),
            )
        )
        val gutterPlaceable = if (hasGutter) {
            measurables[1].measure(
                Constraints(
                    minWidth = gutterWidthPx,
                    maxWidth = gutterWidthPx,
                    minHeight = textPlaceable.height,
                    maxHeight = textPlaceable.height,
                )
            )
        } else null

        layout(constraints.maxWidth, textPlaceable.height) {
            gutterPlaceable?.placeRelative(0, 0)
            textPlaceable.placeRelative(gutterWidthPx, 0)
        }
    }
}

@Composable
private fun LineNumberGutter(
    textLayoutResult: TextLayoutResult?,
    text: String,
    startLineNumber: Int,
    maxLineNumber: Int,
) {
    val density = LocalDensity.current
    val lineNumberColor = MaterialTheme.colorScheme.onSurface
    val digitCount = digitCountForMaxLine(maxLineNumber)
    val paint = remember(digitCount, lineNumberColor) {
        Paint().apply {
            isAntiAlias = true
            textSize = with(density) {
                (if (digitCount >= 4) LineNumberTextSizeSmall else LineNumberTextSize).toPx()
            }
            color = lineNumberColor.toArgb()
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = LineNumberGutterPadding)
            .clearAndSetSemantics { },
        onDraw = drawScope@{
            val layout = textLayoutResult ?: return@drawScope
            val gutterWidthPx = size.width
            var currentLogicalLine = startLineNumber

            for (visualLine in 0 until layout.lineCount) {
                val isNewLogicalLine = if (visualLine == 0) {
                    true
                } else {
                    val lineStart = layout.getLineStart(visualLine)
                    lineStart > 0 && text.getOrNull(lineStart - 1) == '\n'
                }

                if (isNewLogicalLine) {
                    val label = currentLogicalLine.toString()
                    val textWidth = paint.measureText(label)
                    val x = gutterWidthPx - textWidth
                    val y = layout.getLineBaseline(visualLine)
                    drawContext.canvas.nativeCanvas.drawText(label, x, y, paint)
                    currentLogicalLine++
                }
            }
        },
    )
}

private fun editorTextStyle(color: Color): TextStyle = TextStyle(
    color = color,
    fontSize = 14.sp,
    lineHeight = EditorLineHeight,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Proportional,
        trim = LineHeightStyle.Trim.None,
    ),
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private fun digitCountForMaxLine(maxLineNumber: Int): Int =
    maxLineNumber.coerceAtLeast(1).toString().length

package mega.privacy.android.feature.texteditor.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import mega.privacy.android.feature.texteditor.components.markdown.rich.RichSpan
import mega.privacy.android.feature.texteditor.components.markdown.rich.RichSpanStyle
import mega.privacy.android.feature.texteditor.components.markdown.rich.RichTextBlockState

/**
 * Holds an editor field's layout provider for the chip painter. Plain, not snapshot state, for
 * the same reason as [mega.privacy.android.feature.texteditor.components.markdown.rich.RichSelectionGeometry.textLayoutProviders]:
 * `onTextLayout` runs inside the field's measure pass, where a snapshot write re-dirties layout
 * every frame and Compose never goes idle.
 */
internal class InlineCodeChipLayout {
    var provider: (() -> TextLayoutResult?)? = null
}

/**
 * Paints [state]'s inline-code chips behind an editor field. Must come LAST in the modifier
 * chain so the draw origin matches the text's, after any content padding.
 */
internal fun Modifier.inlineCodeChips(
    state: RichTextBlockState,
    layout: InlineCodeChipLayout,
    fill: Color,
    border: Color,
) = drawBehind {
    // Reading both snapshot sources is what schedules the repaint after an edit — the layout
    // provider itself is a plain field and observes nothing.
    val text = state.textFieldState.text
    val ranges = state.spans.inlineCodeRanges()
    if (ranges.isEmpty() || text.isEmpty()) return@drawBehind
    // Span offsets are buffer offsets, which equal visual offsets only because the output
    // transformation restyles without ever inserting or deleting characters.
    val result = layout.provider?.invoke() ?: return@drawBehind
    drawInlineCodeChips(result, ranges, fill, border)
}

/** Code spans of a preview [AnnotatedString], identified by the style the builder applied. */
internal fun AnnotatedString.inlineCodeRanges(codeStyle: SpanStyle): List<TextRange> =
    spanStyles.filter { it.item == codeStyle }.map { TextRange(it.start, it.end) }

/** Code spans of an editor block. */
internal fun List<RichSpan>.inlineCodeRanges(): List<TextRange> =
    filter { it.style == RichSpanStyle.Code }.map { TextRange(it.start, it.end) }

/**
 * Paints the rounded, bordered box design specifies for inline code. A [SpanStyle] carries
 * neither a border nor a corner radius, so the box is drawn behind the text from its layout;
 * the span itself only supplies the monospace font and the brand colour.
 *
 * A run that wraps is drawn as one box per line, so it reads as two tokens rather than one
 * rectangle swallowing the line break.
 */
internal fun DrawScope.drawInlineCodeChips(
    layout: TextLayoutResult,
    ranges: List<TextRange>,
    fill: Color,
    border: Color,
) {
    if (ranges.isEmpty() || layout.lineCount == 0) return
    val radius = CornerRadius(ChipCornerRadius.toPx())
    val strokeWidth = ChipBorderWidth.toPx()
    val padding = ChipHorizontalPadding.toPx()
    val margin = ChipHorizontalMargin.toPx()
    val textLength = layout.layoutInput.text.length

    ranges.forEach { range ->
        val start = range.min.coerceIn(0, textLength)
        val end = range.max.coerceIn(start, textLength)
        if (start == end) return@forEach

        for (line in layout.getLineForOffset(start)..layout.getLineForOffset(end - 1)) {
            val firstOnLine = layout.getLineStart(line)
            val lastOnLine = layout.getLineEnd(line, visibleEnd = true)
            val lineStart = maxOf(start, firstOnLine)
            val lineEnd = minOf(end, lastOnLine)
            if (lineEnd <= lineStart) continue

            val startX = layout.getHorizontalPosition(lineStart, usePrimaryDirection = true)
            val endX = layout.getHorizontalPosition(lineEnd, usePrimaryDirection = true)
            val glyphLeft = minOf(startX, endX)
            val glyphRight = maxOf(startX, endX)
            // The box grows by the padding, but a word space is narrower than that, so it is
            // also held back from the neighbouring glyph by the margin. The padding is what
            // it can be, the margin is what it must be.
            var left = glyphLeft - padding
            var right = glyphRight + padding
            if (lineStart > firstOnLine) {
                val before = layout.getHorizontalPosition(lineStart - 1, usePrimaryDirection = true)
                left = maxOf(left, minOf(before, glyphLeft) + margin)
            }
            if (lineEnd < lastOnLine) {
                val after = layout.getHorizontalPosition(lineEnd + 1, usePrimaryDirection = true)
                right = minOf(right, maxOf(after, glyphRight) - margin)
            }
            if (right <= left) continue
            val top = layout.getLineTop(line)
            val bottom = layout.getLineBottom(line)

            val inset = strokeWidth / 2f
            val topLeft = Offset(left + inset, top + inset)
            val size = Size(
                width = (right - left - strokeWidth).coerceAtLeast(0f),
                height = (bottom - top - strokeWidth).coerceAtLeast(0f),
            )
            drawRoundRect(color = fill, topLeft = topLeft, size = size, cornerRadius = radius)
            drawRoundRect(
                color = border,
                topLeft = topLeft,
                size = size,
                cornerRadius = radius,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

private val ChipCornerRadius = 4.dp
private val ChipBorderWidth = 1.dp
private val ChipHorizontalPadding = 4.dp
private val ChipHorizontalMargin = 2.dp

package mega.privacy.android.shared.original.core.ui.controls.lists

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import java.text.NumberFormat
import java.util.Locale

/**
 * A composable that displays a numbered list of strings.
 * Each item in the list is prefixed with its number.
 * @param list The list of strings to display.
 * @param modifier The [Modifier] to be applied to the list.
 * @param maxLines The maximum number of lines to display before truncating.
 * @param textColor The color of the text.
 * @param style The [TextStyle] to be applied to the text.
 */
@Composable
fun NumberedListView(
    list: List<String>,
    modifier: Modifier = Modifier,
    maxLines: Int = 50,
    textColor: TextColor = TextColor.Secondary,
    style: TextStyle = MaterialTheme.typography.body1,
    itemSpacing: Dp = 10.dp,
) {
    MegaText(
        text = makeNumberedAnnotatedString(
            items = list,
            style = style,
            itemSpacing = itemSpacing
        ),
        maxLines = maxLines,
        textColor = textColor,
        style = style,
        modifier = modifier,
    )
}

@Composable
private fun makeNumberedAnnotatedString(
    items: List<String>,
    style: TextStyle = LocalTextStyle.current,
    lineBreak: LineBreak = LineBreak.Paragraph,
    itemSpacing: Dp = 10.dp,
): AnnotatedString {
    val numberFormatter = remember {
        NumberFormat.getInstance(Locale.getDefault())
    }
    val textMeasurer = rememberTextMeasurer()
    val monospaceStyle = style.copy(
        fontFamily = FontFamily.Monospace
    )

    val markerLen = numberFormatter.format(items.size).length
    val monospaceSize = textMeasurer.measure(text = " ", style = monospaceStyle).size
    val spaceSize = textMeasurer.measure(text = " ", style = style).size
    val dotSize = textMeasurer.measure(text = ".", style = style).size

    return buildAnnotatedString {
        items.forEachIndexed { index, text ->
            val count = (index + 1).toString().length
            val tailLen = markerLen - count
            withStyle(
                style = ParagraphStyle(
                    textIndent = TextIndent(restLine = with(LocalDensity.current) {
                        (monospaceSize.width * markerLen + dotSize.width + spaceSize.width).toSp()
                    }),
                    lineHeight = with(LocalDensity.current) { monospaceSize.height.toSp() },
                    lineBreak = lineBreak
                )
            ) {
                withStyle(monospaceStyle.toSpanStyle()) {
                    append(numberFormatter.format(index + 1))
                }
                append(".")
                if (tailLen > 0) {
                    withStyle(monospaceStyle.toSpanStyle()) {
                        append("".padEnd(tailLen))
                    }
                }
                append(" ")
                append(text)
            }
            if (index < items.lastIndex) {
                val spacing = with(LocalDensity.current) { itemSpacing.toSp() }
                withStyle(
                    style = SpanStyle(
                        fontSize = spacing,
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append("\u200B") // Add a new line for spacing
                }
            }
        }
    }
}
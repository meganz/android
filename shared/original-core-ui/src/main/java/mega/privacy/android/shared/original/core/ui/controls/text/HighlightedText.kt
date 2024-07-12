package mega.privacy.android.shared.original.core.ui.controls.text

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.red_200
import mega.privacy.android.shared.original.core.ui.theme.teal_300_alpha_020
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * @param text Text to show
 * @param highlightText Text to background highlight
 * @param textColor Text color
 * @param modifier [Modifier]
 * @param highlightColor Optional color for background highlight
 * @param highlightBold Optional bold font highlight
 * @param maxLines Minimum lines
 * @param style Text style
 * @param overflow Overflow option
 */
@Composable
fun HighlightedText(
    text: String,
    highlightText: String,
    textColor: TextColor,
    modifier: Modifier = Modifier,
    highlightColor: Color = teal_300_alpha_020,
    highlightBold: Boolean = false,
    maxLines: Int = 1,
    style: TextStyle = LocalTextStyle.current,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val annotatedText: AnnotatedString = buildAnnotatedString {
        append(text)
        var startIndex = text.indexOf(string = highlightText, ignoreCase = true)
        while (startIndex >= 0) {
            val endIndex = startIndex + highlightText.length
            addStyle(
                style = SpanStyle(
                    background = highlightColor,
                    fontWeight = if (highlightBold) FontWeight.Bold else FontWeight.Normal
                ),
                start = startIndex,
                end = endIndex
            )
            startIndex = text.indexOf(
                string = highlightText,
                startIndex = startIndex + highlightText.length,
                ignoreCase = true
            )
        }
    }

    Text(
        text = annotatedText,
        modifier = modifier,
        overflow = overflow,
        maxLines = maxLines,
        style = style,
        color = MegaOriginalTheme.textColor(textColor = textColor),
    )
}

@CombinedThemePreviews
@Composable
private fun HighlightedTextPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        HighlightedText(
            text = "This is a title with Title highlight",
            highlightText = "TITLE",
            textColor = TextColor.Primary,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun HighlightedTextBoldPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        HighlightedText(
            text = "This is a title with TITLE highlight",
            highlightText = "TITLE",
            textColor = TextColor.Primary,
            highlightColor = red_200,
            highlightBold = true,
        )
    }
}

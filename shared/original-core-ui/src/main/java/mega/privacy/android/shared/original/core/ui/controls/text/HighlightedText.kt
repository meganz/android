package mega.privacy.android.shared.original.core.ui.controls.text

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.InlineTextContent
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
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.red_200
import mega.privacy.android.shared.original.core.ui.utils.normalize

/**
 * @param text Text to show
 * @param highlightText Text to background highlight
 * @param textColor Text color
 * @param modifier [Modifier]
 * @param highlightColor Optional color for background highlight
 * @param highlightFontWeight Optional font weight for highlight
 * @param maxLines Minimum lines
 * @param style Text style
 * @param overflow Overflow option
 */
@Composable
fun HighlightedText(
    text: String,
    highlightText: String,
    modifier: Modifier = Modifier,
    textColor: TextColor = TextColor.Primary,
    highlightColor: Color = MegaOriginalTheme.colors.notifications.notificationSuccess,
    highlightFontWeight: FontWeight = FontWeight.Normal,
    applyMarqueEffect: Boolean = true,
    maxLines: Int = 1,
    style: TextStyle = LocalTextStyle.current,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    HighlightedText(
        text = AnnotatedString(text),
        highlightText = highlightText,
        modifier = modifier,
        textColor = textColor,
        highlightColor = highlightColor,
        highlightFontWeight = highlightFontWeight,
        applyMarqueEffect = applyMarqueEffect,
        maxLines = maxLines,
        style = style,
        overflow = overflow,
    )
}

/**
 * @param text Annotated string to show
 * @param highlightText Text to background highlight
 * @param textColor Text color
 * @param modifier [Modifier]
 * @param highlightColor Optional color for background highlight
 * @param highlightFontWeight Optional font weight for highlight
 * @param maxLines Minimum lines
 * @param style Text style
 * @param overflow Overflow option
 */
@Composable
fun HighlightedText(
    text: AnnotatedString,
    highlightText: String,
    modifier: Modifier = Modifier,
    textColor: TextColor = TextColor.Primary,
    highlightColor: Color = MegaOriginalTheme.colors.notifications.notificationSuccess,
    highlightFontWeight: FontWeight = FontWeight.Normal,
    applyMarqueEffect: Boolean = true,
    maxLines: Int = 1,
    style: TextStyle = LocalTextStyle.current,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
) {
    if (text.isEmpty()) return

    if (highlightText.isEmpty()) {
        Text(
            text = text,
            modifier = modifier.basicMarquee(),
            maxLines = maxLines,
            overflow = overflow,
            style = style,
            color = MegaOriginalTheme.textColor(textColor = textColor),
        )
        return
    }

    val annotatedText: AnnotatedString = buildAnnotatedString {
        append(text)
        val normalizedHighlight = highlightText.normalize()
        val normalizedText = text.text.normalize()
        var startIndex = normalizedText.indexOf(string = normalizedHighlight, ignoreCase = true)
        while (startIndex >= 0) {
            val endIndex = startIndex + normalizedHighlight.length
            if (endIndex <= text.length) {
                addStyle(
                    style = SpanStyle(
                        background = highlightColor,
                        fontWeight = highlightFontWeight,
                    ),
                    start = startIndex,
                    end = endIndex
                )
            } else {
                break
            }
            startIndex = normalizedText.indexOf(
                string = normalizedHighlight,
                startIndex = endIndex,
                ignoreCase = true
            )
        }
    }

    Text(
        text = annotatedText,
        modifier = modifier.then(if (applyMarqueEffect) Modifier.basicMarquee() else Modifier),
        maxLines = maxLines,
        overflow = overflow,
        style = style,
        color = MegaOriginalTheme.textColor(textColor = textColor),
        inlineContent = inlineContent,
    )
}

@CombinedThemePreviews
@Composable
private fun HighlightedTextPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        HighlightedText(
            text = "This is ä tìtle with TITLE highlight",
            highlightText = "TITLE",
            textColor = TextColor.Primary,
            highlightColor = red_200,
            highlightFontWeight = FontWeight.Bold,
        )
    }
}

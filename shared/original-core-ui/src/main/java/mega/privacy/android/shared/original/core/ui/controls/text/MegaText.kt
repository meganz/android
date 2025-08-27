package mega.privacy.android.shared.original.core.ui.controls.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeComponentPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.textColor

/**
 * Mega text.
 *
 * @param text Text to show.
 * @param textColor Text color.
 * @param modifier [Modifier].
 * @param overflow Text overflow.
 * @param minLines Min lines.
 * @param style Text style.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MegaText(
    text: String,
    textColor: TextColor,
    modifier: Modifier = Modifier,
    overflow: LongTextBehaviour = LongTextBehaviour.Clip(),
    minLines: Int = 1,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    softWrap: Boolean = true,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) = when (overflow) {
    LongTextBehaviour.MiddleEllipsis -> MiddleEllipsisText(
        text = text,
        color = textColor,
        modifier = modifier,
        maxLines = overflow.maxLines,
        style = style,
        textAlign = textAlign,
        softWrap = softWrap,
        onTextLayout = onTextLayout
    )

    LongTextBehaviour.Marquee -> MarqueeText(
        text = text,
        modifier = Modifier.basicMarquee(),
        color = DSTokens.textColor(textColor = textColor),
        style = style,
        textAlign = textAlign,
        softWrap = softWrap,
        onTextLayout = onTextLayout
    )

    else -> Text(
        text = text,
        modifier = modifier,
        color = DSTokens.textColor(textColor = textColor),
        overflow = overflow.getTextOverflow(),
        maxLines = overflow.maxLines,
        minLines = minLines,
        style = style,
        textAlign = textAlign,
        softWrap = softWrap,
        onTextLayout = onTextLayout
    )
}

/**
 * Mega text.
 *
 * @param text AnnotatedString.
 * @param textColor Text color.
 * @param modifier [Modifier].
 * @param inlineContent Map<String, InlineTextContent> A map store composables that replaces certain ranges of the text.
 * @param maxLines Max lines.
 * @param minLines Min lines.
 * @param style Text style.
 * @param textAlign The alignment of the text within the lines of the paragraph.
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
 */
@Composable
fun MegaText(
    text: AnnotatedString,
    textColor: TextColor,
    maxLines: Int,
    modifier: Modifier = Modifier,
    overflow: TextOverflow = TextOverflow.Clip,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    minLines: Int = 1,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) = Text(
    text = text,
    modifier = modifier,
    overflow = overflow,
    color = DSTokens.textColor(textColor = textColor),
    maxLines = maxLines,
    minLines = minLines,
    style = style,
    textAlign = textAlign,
    inlineContent = inlineContent,
    onTextLayout = onTextLayout
)

/**
 * Mega text overflow mapper.
 */
private fun LongTextBehaviour.getTextOverflow() = when (this) {
    is LongTextBehaviour.Clip -> TextOverflow.Clip
    is LongTextBehaviour.Ellipsis -> TextOverflow.Ellipsis
    is LongTextBehaviour.Visible -> TextOverflow.Visible
    is LongTextBehaviour.MiddleEllipsis -> TextOverflow.Visible
    is LongTextBehaviour.Marquee -> TextOverflow.Visible
}

@CombinedThemeComponentPreviews
@Composable
private fun PreviewMiddleEllipsisText(
    @PreviewParameter(MiddleEllipsisTextPreviewProvider::class) text: String,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaText(
            text = text,
            textColor = TextColor.Primary,
            overflow = LongTextBehaviour.MiddleEllipsis,
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun MarqueeTextPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaText(
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua",
            textColor = TextColor.Secondary,
            overflow = LongTextBehaviour.Marquee
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun MegaTextPreview(
    @PreviewParameter(TextColorProvider::class) textColor: TextColor,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaText(
            text = textColor.name,
            textColor = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

private class TextColorProvider : PreviewParameterProvider<TextColor> {
    override val values = TextColor.values().asSequence()
}

private class MiddleEllipsisTextPreviewProvider : PreviewParameterProvider<String> {
    override val values = listOf(
        "SoooooooooooooooooooooooLoooooooooooooooooooongText",
        "\uD83D\uDE00".repeat(20),
        "Normal Text"
    ).asSequence()
}
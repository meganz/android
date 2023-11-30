package mega.privacy.android.core.ui.controls.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.tokens.TextColor

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
) = when (overflow) {
    LongTextBehaviour.MiddleEllipsis -> MiddleEllipsisText(
        text = text,
        color = textColor,
        modifier = modifier,
        maxLines = overflow.maxLines,
        style = style
    )

    LongTextBehaviour.Marquee -> MarqueeText(
        text = text,
        modifier = Modifier.basicMarquee(),
        color = MegaTheme.textColor(textColor = textColor),
        style = style,
    )

    else -> Text(
        text = text,
        modifier = modifier,
        color = MegaTheme.textColor(textColor = textColor),
        overflow = overflow.getTextOverflow(),
        maxLines = overflow.maxLines,
        minLines = minLines,
        style = style,
    )
}

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

@CombinedThemePreviews
@Composable
private fun PreviewMiddleEllipsisText(
    @PreviewParameter(MiddleEllipsisTextPreviewProvider::class) text: String,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaText(
            text = text,
            textColor = TextColor.Primary,
            overflow = LongTextBehaviour.MiddleEllipsis,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MarqueeTextPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaText(
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua",
            textColor = TextColor.Secondary,
            overflow = LongTextBehaviour.Marquee
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MegaTextPreview(
    @PreviewParameter(TextColorProvider::class) textColor: TextColor,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
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
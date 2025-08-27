package mega.privacy.android.shared.original.core.ui.controls.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.textColor

/**
 * Marquee text.
 *
 * @param text Text to show.
 * @param color Text color.
 * @param modifier [Modifier].
 * @param style Text style (color will be ignored).
 */
@Composable
fun MarqueeText(
    text: String,
    color: TextColor,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    softWrap: Boolean = true,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) = MarqueeText(
    text = text,
    color = DSTokens.textColor(color),
    modifier = modifier,
    style = style,
    softWrap = softWrap,
    onTextLayout = onTextLayout
)

/**
 * Marquee text.
 *
 * @param text Text to show.
 * @param modifier [Modifier].
 * @param color Text color.
 * @param style Text style.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    softWrap: Boolean = true,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) = Text(
    text = text,
    modifier = modifier.basicMarquee(),
    color = color,
    maxLines = 1,
    style = style,
    textAlign = textAlign,
    softWrap = softWrap,
    onTextLayout = onTextLayout
)

@CombinedThemePreviews
@Composable
private fun MarqueeTextPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MarqueeText(
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua",
            color = DSTokens.colors.text.secondary
        )
    }
}
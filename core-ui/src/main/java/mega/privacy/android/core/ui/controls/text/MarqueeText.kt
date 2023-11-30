package mega.privacy.android.core.ui.controls.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.tokens.TextColor

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
) = MarqueeText(
    text = text,
    color = MegaTheme.textColor(color),
    modifier = modifier,
    style = style
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
) = Text(
    text = text,
    modifier = modifier.basicMarquee(),
    color = color,
    maxLines = 1,
    style = style,
)

@CombinedThemePreviews
@Composable
private fun MarqueeTextPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MarqueeText(
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua",
            color = MegaTheme.colors.text.secondary
        )
    }
}
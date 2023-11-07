package mega.privacy.android.legacy.core.ui.controls.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme


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
@Deprecated(
    message = "This has been deprecated in favour of MarqueeText core component in core-ui module that uses TextColor to ensure usage of design system color tokens",
    replaceWith = ReplaceWith("mega.privacy.android.core.ui.controls.text.MarqueeText")
)
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) = Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Text(
        text,
        modifier = Modifier.basicMarquee(),
        color = color,
        maxLines = 1,
        style = style,
    )
}

@CombinedThemePreviews
@Composable
private fun MarqueeTextPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MarqueeText(
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua",
            color = Color.Black
        )
    }
}
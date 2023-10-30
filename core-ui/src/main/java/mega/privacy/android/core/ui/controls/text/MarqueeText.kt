package mega.privacy.android.core.ui.controls.text

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
import mega.privacy.android.core.ui.theme.MegaTheme

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
            color = MegaTheme.colors.text.secondary
        )
    }
}
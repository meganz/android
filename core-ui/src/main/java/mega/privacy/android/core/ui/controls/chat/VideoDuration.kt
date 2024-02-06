package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.body4
import mega.privacy.android.core.ui.theme.tokens.TextColor

/**
 * Small text to show video duration in chat attachment messages
 */
@Composable
fun VideoDuration(
    duration: String,
    modifier: Modifier = Modifier,
) = MegaText(
    modifier = modifier
        .background(Color.Black.copy(alpha = 0.7f), shape = CircleShape)
        .padding(horizontal = 6.dp, vertical = 3.5.dp),
    text = duration,
    textColor = TextColor.OnColor,
    style = MaterialTheme.typography.body4.copy(fontSize = 10.sp)
)

@CombinedThemePreviews
@Composable
private fun VideoDurationPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        VideoDuration(
            duration = "01:34"
        )
    }
}
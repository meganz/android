package mega.privacy.android.core.ui.controls.chat.messages.file

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.chat.VideoDuration
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Overlay for previews of playable files (usually videos) to better indicate that this file can be played.
 * @param duration
 */
@Composable
fun PlayPreviewOverlay(
    duration: String,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
) {

    Image(
        modifier = Modifier
            .align(Alignment.Center)
            .background(Color.Black.copy(alpha = 0.7f), shape = CircleShape)
            .padding(4.dp)
            .size(32.dp),
        colorFilter = ColorFilter.tint(MegaTheme.colors.icon.onColor),
        painter = painterResource(id = R.drawable.ic_play_circle_medium_regular_outline),
        contentDescription = "Play"
    )
    VideoDuration(
        duration = duration,
        modifier = Modifier
            .padding(8.dp)
            .align(Alignment.BottomEnd)
    )
}

@Composable
@CombinedThemePreviews
private fun PlayPreviewOverlayPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        PlayPreviewOverlay(
            modifier = Modifier
                .background(Color.Gray)
                .size(212.dp),
            duration = "01:34"
        )
    }
}

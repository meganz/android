package mega.privacy.android.shared.original.core.ui.controls.chat.messages.file

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.chat.VideoDuration
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Overlay for previews of playable files (usually videos) to better indicate that this file can be played.
 * @param duration
 */
@Composable
fun PlayPreviewOverlay(
    duration: String,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier.background(Color.Black.copy(alpha = 0.5f))
) {

    Image(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(4.dp)
            .size(32.dp),
        colorFilter = ColorFilter.tint(DSTokens.colors.icon.onColor),
        painter = rememberVectorPainter(IconPack.Medium.Thin.Solid.PlayCircle),
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        PlayPreviewOverlay(
            modifier = Modifier
                .background(Color.Gray)
                .size(212.dp),
            duration = "01:34"
        )
    }
}

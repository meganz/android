package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.feature.mediaplayer.components.MediaPlayerLoadingIndicator
import mega.privacy.android.icon.pack.IconPack

/**
 * Play/pause toggle for the video player.
 *
 * Deliberately not the audio player's `PlayPauseButton`. That one draws a bare glyph on a surface
 * that follows the theme; this one is drawn over the picture and uses the disc-backed icons, which
 * carry their own contrast and are what the live video player shows.
 *
 * [isLoading] swaps the icon for the throbber, so the screen never has two spinners on it. The
 * button stays tappable throughout, which the live player does not allow — recorded as a deviation
 * in `.ref/video-player-feature-inventory.md` 6.13.
 */
@Composable
internal fun VideoPlayerPlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(BUTTON_SIZE),
    ) {
        if (isLoading) {
            MediaPlayerLoadingIndicator(modifier = Modifier.size(THROBBER_SIZE))
        } else {
            MegaIcon(
                imageVector =
                    if (isPlaying) IconPack.Medium.Thin.Solid.PauseCircle
                    else IconPack.Medium.Thin.Solid.PlayCircle,
                tint = IconColor.OnColor,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(BUTTON_SIZE),
            )
        }
    }
}

/** What the live player lays `play_pause_placeholder` out to. */
private val BUTTON_SIZE = 56.dp

/** What the live player lays its `BarSpinnerView` out to, centred on the button. */
private val THROBBER_SIZE = 48.dp

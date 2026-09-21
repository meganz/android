package mega.privacy.android.feature.mediaplayer.presentation.miniplayer

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.mediaplayer.components.AudioMiniPlayerProgressBar
import mega.privacy.android.feature.mediaplayer.components.audioMiniPlayerIconColor
import mega.privacy.android.feature.mediaplayer.components.MediaPlayerLoadingIndicator
import mega.privacy.android.icon.pack.IconPack

// Matches R.dimen.audio_player_mini_controller_height, the height the PlayerView bar reserves in
// every host; the resource lives in :app and cannot be read from here.
private val MiniPlayerHeight = 64.dp
private val PlayPauseTouchTarget = 40.dp
private val PlayPauseIconSize = 24.dp
private val LoadingIndicatorSize = 32.dp

/**
 * Compose mini player for the revamped audio player.
 *
 * Stateless: the host owns [state] and every action is a lambda. Replaces the Media3 `PlayerView`
 * in `mini_audio_player.xml`, which is shared with the legacy player and therefore cannot be
 * changed for V2 alone.
 *
 * Not used by any host yet: the XML bar has to stay in place to serve legacy sessions, so the
 * hosts will pick between the two behind a feature flag once the revamped player has shipped.
 *
 * @param state What to render.
 * @param onPlayPauseClicked Called when the play/pause button is tapped.
 * @param onCloseClicked Called when the close button is tapped.
 * @param onPlayerClicked Called when the bar itself is tapped, to open the full-screen player.
 * @param modifier Modifier.
 */
@Composable
internal fun AudioMiniPlayer(
    state: AudioMiniPlayerUiState.Data,
    onPlayPauseClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    onPlayerClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxSurface(
        surfaceColor = SurfaceColor.Surface1,
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .clickable(onClick = onPlayerClicked),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayPauseControl(
                isPlaying = state.isPlaying,
                isLoading = state.isLoading,
                onClick = onPlayPauseClicked,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                MegaText(
                    text = state.title,
                    style = MaterialTheme.typography.titleMedium,
                    textColor = TextColor.Primary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
                if (!state.artist.isNullOrBlank()) {
                    MegaText(
                        text = state.artist,
                        style = MaterialTheme.typography.bodySmall,
                        textColor = TextColor.Secondary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                    )
                }
            }

            IconButton(onClick = onCloseClicked) {
                MegaIcon(
                    imageVector = IconPack.Medium.Thin.Outline.X,
                    tint = IconColor.Secondary,
                    contentDescription = "Close",
                )
            }
        }

        AudioMiniPlayerProgressBar(
            progress = state.progress,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Play/pause toggle of the mini player. While buffering the icon is swapped for a throbber, but
 * the button stays tappable so a mid-playback rebuffer never removes the pause affordance.
 */
@Composable
private fun PlayPauseControl(
    isPlaying: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(PlayPauseTouchTarget),
    ) {
        if (isLoading) {
            MediaPlayerLoadingIndicator(
                modifier = Modifier.size(LoadingIndicatorSize),
                color = audioMiniPlayerIconColor(),
            )
        } else {
            MegaIcon(
                imageVector =
                    if (isPlaying) IconPack.Medium.Regular.Solid.Pause
                    else IconPack.Medium.Regular.Solid.Play,
                tint = IconColor.Primary,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(PlayPauseIconSize),
            )
        }
    }
}

@Preview
@Composable
private fun AudioMiniPlayerPlayingPreview() {
    AndroidThemeForPreviews {
        AudioMiniPlayer(
            state = AudioMiniPlayerUiState.Data(
                title = "Pioneer To The Falls (Live)",
                artist = "Interpol",
                isPlaying = true,
                isLoading = false,
                progress = 0.35f,
            ),
            onPlayPauseClicked = {},
            onCloseClicked = {},
            onPlayerClicked = {},
        )
    }
}

@Preview
@Composable
private fun AudioMiniPlayerLoadingPreview() {
    AndroidThemeForPreviews {
        AudioMiniPlayer(
            state = AudioMiniPlayerUiState.Data(
                title = "Orange (Live)",
                artist = null,
                isPlaying = false,
                isLoading = true,
                progress = 0f,
            ),
            onPlayPauseClicked = {},
            onCloseClicked = {},
            onPlayerClicked = {},
        )
    }
}

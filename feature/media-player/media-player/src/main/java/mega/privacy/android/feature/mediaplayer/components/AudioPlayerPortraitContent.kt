package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData

/**
 * Portrait layout for the audio player.
 *
 * Displays the artwork in a square section that fills the available vertical space,
 * followed by track metadata, seek bar, playback controls (podcast or music mode),
 * and primary actions pinned at the bottom. The gradient background spans the full screen.
 */
@Composable
fun AudioPlayerPortraitContent(
    isPlaying: Boolean,
    isLoading: Boolean,
    controlsEnabled: Boolean,
    nextEnabled: Boolean,
    currentPosition: Long,
    duration: Long,
    title: String,
    artist: String?,
    artworkUri: String?,
    thumbnailData: ThumbnailData?,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    isPodcastMode: Boolean,
    currentPlaybackSpeed: Float,
    sleepTimerLabel: String?,
    sleepTimerActive: Boolean,
    sleepTimerContentDescription: String,
    onPlayPauseClicked: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onRepeatClicked: () -> Unit,
    onPlaylistClicked: () -> Unit,
    onToggleMode: () -> Unit,
    onSeekForward15: () -> Unit,
    onSeekBackward15: () -> Unit,
    onSpeedClicked: () -> Unit,
    onSleepTimerClicked: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    AudioPlayerGradientBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding(), start = 16.dp, end = 16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            AudioArtworkSection(
                artworkUri = artworkUri,
                thumbnailData = thumbnailData,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            AudioMetadataSection(
                title = title,
                artist = artist,
            )

            Spacer(modifier = Modifier.height(24.dp))

            AudioPlayerController(
                currentPosition = currentPosition,
                duration = duration,
                isPlaying = isPlaying,
                isLoading = isLoading,
                controlsEnabled = controlsEnabled,
                nextEnabled = nextEnabled,
                isPodcastMode = isPodcastMode,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                currentPlaybackSpeed = currentPlaybackSpeed,
                sleepTimerLabel = sleepTimerLabel,
                sleepTimerActive = sleepTimerActive,
                sleepTimerContentDescription = sleepTimerContentDescription,
                onSeekTo = onSeekTo,
                onPlayPauseClicked = onPlayPauseClicked,
                onNextClicked = onNextClicked,
                onPreviousClicked = onPreviousClicked,
                onShuffleClicked = onShuffleClicked,
                onRepeatClicked = onRepeatClicked,
                onToggleMode = onToggleMode,
                onPlaylistClicked = onPlaylistClicked,
                onSeekForward15 = onSeekForward15,
                onSeekBackward15 = onSeekBackward15,
                onSpeedClicked = onSpeedClicked,
                onSleepTimerClicked = onSleepTimerClicked,
            )
        }
    }
}

@Preview(name = "Portrait – no artwork")
@Composable
private fun PreviewAudioPlayerPortraitContent() {
    AndroidTheme(isDark = true) {
        AudioPlayerPortraitContent(
            isPlaying = false,
            isLoading = false,
            controlsEnabled = true,
            nextEnabled = true,
            currentPosition = 45_000L,
            duration = 240_000L,
            title = "Song Title",
            artist = "Artist Name",
            artworkUri = null,
            thumbnailData = null,
            repeatMode = Player.REPEAT_MODE_OFF,
            shuffleEnabled = false,
            isPodcastMode = false,
            currentPlaybackSpeed = 1f,
            sleepTimerLabel = null,
            sleepTimerActive = false,
            sleepTimerContentDescription = "Sleep timer",
            onPlayPauseClicked = {},
            onSeekTo = {},
            onNextClicked = {},
            onPreviousClicked = {},
            onShuffleClicked = {},
            onRepeatClicked = {},
            onPlaylistClicked = {},
            onToggleMode = {},
            onSeekForward15 = {},
            onSeekBackward15 = {},
            onSpeedClicked = {},
            onSleepTimerClicked = {},
            contentPadding = PaddingValues(),
        )
    }
}

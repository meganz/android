package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Composite controller containing the seek bar, playback controls (podcast or music mode),
 * and the primary actions row (mode toggle + playlist).
 *
 * The landscape layout positions the playback controls separately (vertically centred), so it
 * composes [AudioPlayerSeekBar], [PodcastPlaybackControls] / [MusicPlaybackControls], and
 * [AudioPlayerPrimaryActions] directly.
 */
@Composable
internal fun AudioPlayerController(
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    isLoading: Boolean,
    controlsEnabled: Boolean,
    nextEnabled: Boolean,
    isPodcastMode: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    currentPlaybackSpeed: Float,
    sleepTimerLabel: String?,
    sleepTimerActive: Boolean,
    sleepTimerContentDescription: String,
    onSeekTo: (Long) -> Unit,
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onRepeatClicked: () -> Unit,
    onToggleMode: () -> Unit,
    onPlaylistClicked: () -> Unit,
    onSeekForward15: () -> Unit,
    onSeekBackward15: () -> Unit,
    onSpeedClicked: () -> Unit,
    onSleepTimerClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AudioPlayerSeekBar(
            currentPosition = currentPosition,
            duration = duration,
            onSeekTo = onSeekTo,
            sleepTimerLabel = sleepTimerLabel,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isPodcastMode) {
            PodcastPlaybackControls(
                isPlaying = isPlaying,
                isLoading = isLoading,
                controlsEnabled = controlsEnabled,
                currentPlaybackSpeed = currentPlaybackSpeed,
                sleepTimerActive = sleepTimerActive,
                sleepTimerContentDescription = sleepTimerContentDescription,
                onSpeedClicked = onSpeedClicked,
                onSeekBackward15 = onSeekBackward15,
                onPlayPauseClicked = onPlayPauseClicked,
                onSeekForward15 = onSeekForward15,
                onSleepTimerClicked = onSleepTimerClicked,
            )
        } else {
            MusicPlaybackControls(
                isPlaying = isPlaying,
                isLoading = isLoading,
                controlsEnabled = controlsEnabled,
                nextEnabled = nextEnabled,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                onShuffleClicked = onShuffleClicked,
                onPreviousClicked = onPreviousClicked,
                onPlayPauseClicked = onPlayPauseClicked,
                onNextClicked = onNextClicked,
                onRepeatClicked = onRepeatClicked,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        AudioPlayerPrimaryActions(
            isPodcastMode = isPodcastMode,
            onToggleMode = onToggleMode,
            onPlaylistClicked = onPlaylistClicked,
        )
    }
}

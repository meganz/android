package mega.privacy.android.app.presentation.audioplayer

import androidx.compose.runtime.Composable
import androidx.media3.common.Player
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.mediaplayer.AudioPlayerScreen
import mega.privacy.android.app.mediaplayer.model.AudioPlayerUiState

/**
 * Baseline screenshots for [AudioPlayerScreen] across every meaningful UI
 * state the composable renders: loading, playing, paused, mid-playback
 * buffering, and active shuffle / repeat-one mode.
 */
class AudioPlayerScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AudioPlayerScreenLoading() {
        AndroidThemeForPreviews {
            AudioPlayerScreen(
                uiState = AudioPlayerUiState.Loading,
                isPodcastMode = true,
                onPlayPauseClicked = {},
                onSeekTo = {},
                onNextClicked = {},
                onPreviousClicked = {},
                onShuffleClicked = {},
                onRepeatClicked = {},
                onPlaylistClicked = {},
                onBackPressed = {},
                onMoreActionsClicked = {},
                onToggleMode = {},
                onSeekForward15 = {},
                onSeekBackward15 = {},
                onSpeedClicked = {},
                onSleepTimerClicked = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AudioPlayerScreenPlaying() {
        AndroidThemeForPreviews {
            AudioPlayerScreen(
                uiState = audioData(
                    isPlaying = true,
                    title = "Bohemian Rhapsody",
                    artist = "Queen",
                    currentPosition = 97_000L,
                    duration = 354_000L,
                ),
                isPodcastMode = false,
                onPlayPauseClicked = {},
                onSeekTo = {},
                onNextClicked = {},
                onPreviousClicked = {},
                onShuffleClicked = {},
                onRepeatClicked = {},
                onPlaylistClicked = {},
                onBackPressed = {},
                onMoreActionsClicked = {},
                onToggleMode = {},
                onSeekForward15 = {},
                onSeekBackward15 = {},
                onSpeedClicked = {},
                onSleepTimerClicked = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AudioPlayerScreenPaused() {
        AndroidThemeForPreviews {
            AudioPlayerScreen(
                uiState = audioData(
                    isPlaying = false,
                    title = "Bohemian Rhapsody",
                    artist = "Queen",
                    currentPosition = 97_000L,
                    duration = 354_000L,
                ),
                isPodcastMode = false,
                onPlayPauseClicked = {},
                onSeekTo = {},
                onNextClicked = {},
                onPreviousClicked = {},
                onShuffleClicked = {},
                onRepeatClicked = {},
                onPlaylistClicked = {},
                onBackPressed = {},
                onMoreActionsClicked = {},
                onToggleMode = {},
                onSeekForward15 = {},
                onSeekBackward15 = {},
                onSpeedClicked = {},
                onSleepTimerClicked = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AudioPlayerScreenBuffering() {
        AndroidThemeForPreviews {
            AudioPlayerScreen(
                uiState = audioData(
                    isLoading = true,
                    title = "Bohemian Rhapsody",
                    artist = "Queen",
                    duration = 354_000L,
                ),
                isPodcastMode = false,
                onPlayPauseClicked = {},
                onSeekTo = {},
                onNextClicked = {},
                onPreviousClicked = {},
                onShuffleClicked = {},
                onRepeatClicked = {},
                onPlaylistClicked = {},
                onBackPressed = {},
                onMoreActionsClicked = {},
                onToggleMode = {},
                onSeekForward15 = {},
                onSeekBackward15 = {},
                onSpeedClicked = {},
                onSleepTimerClicked = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AudioPlayerScreenShuffleAndRepeatOne() {
        AndroidThemeForPreviews {
            AudioPlayerScreen(
                uiState = audioData(
                    isPlaying = true,
                    title = "Bohemian Rhapsody",
                    artist = "Queen",
                    currentPosition = 42_000L,
                    duration = 354_000L,
                    shuffleEnabled = true,
                    repeatMode = Player.REPEAT_MODE_ONE,
                ),
                isPodcastMode = true,
                onPlayPauseClicked = {},
                onSeekTo = {},
                onNextClicked = {},
                onPreviousClicked = {},
                onShuffleClicked = {},
                onRepeatClicked = {},
                onPlaylistClicked = {},
                onBackPressed = {},
                onMoreActionsClicked = {},
                onToggleMode = {},
                onSeekForward15 = {},
                onSeekBackward15 = {},
                onSpeedClicked = {},
                onSleepTimerClicked = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AudioPlayerScreenItemNameFallback() {
        AndroidThemeForPreviews {
            AudioPlayerScreen(
                uiState = audioData(
                    title = null,
                    artist = null,
                    currentPlayingItemName = "podcast_episode_42.mp3",
                    duration = 3_600_000L,
                ),
                isPodcastMode = true,
                onPlayPauseClicked = {},
                onSeekTo = {},
                onNextClicked = {},
                onPreviousClicked = {},
                onShuffleClicked = {},
                onRepeatClicked = {},
                onPlaylistClicked = {},
                onBackPressed = {},
                onMoreActionsClicked = {},
                onToggleMode = {},
                onSeekForward15 = {},
                onSeekBackward15 = {},
                onSpeedClicked = {},
                onSleepTimerClicked = {},
            )
        }
    }

    private fun audioData(
        isPlaying: Boolean = false,
        isLoading: Boolean = false,
        currentPosition: Long = 0L,
        duration: Long = 0L,
        title: String? = null,
        artist: String? = null,
        currentPlayingItemName: String? = null,
        shuffleEnabled: Boolean = false,
        repeatMode: Int = Player.REPEAT_MODE_OFF,
    ) = AudioPlayerUiState.Data(
        isPlaying = isPlaying,
        isLoading = isLoading,
        currentPosition = currentPosition,
        duration = duration,
        title = title,
        artist = artist,
        artworkUri = null,
        repeatMode = repeatMode,
        shuffleEnabled = shuffleEnabled,
        currentPlayingHandle = -1L,
        currentPlayingItemName = currentPlayingItemName,
        hasPlaylist = false,
        currentAdapterType = -1,
        thumbnailData = null,
        currentPlaybackSpeed = 1f,
    )
}

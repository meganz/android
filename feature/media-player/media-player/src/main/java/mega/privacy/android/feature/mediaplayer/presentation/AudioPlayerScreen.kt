package mega.privacy.android.feature.mediaplayer.presentation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.Player
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.mediaplayer.components.AudioPlayerWindowEffect
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.feature.mediaplayer.components.AudioPlayerLandscapeContent
import mega.privacy.android.feature.mediaplayer.components.AudioPlayerPortraitContent
import mega.privacy.android.feature.mediaplayer.presentation.model.AudioPlayerUiState
import mega.privacy.android.feature.mediaplayer.presentation.model.SleepTimerState
import mega.privacy.android.shared.resources.R as sharedR

internal const val AUDIO_PLAYER_CONTENT_TAG = "audio_player:content"

/**
 * Revamped audio player controller screen built with Compose.
 *
 * This is a stateless composable that renders the full audio player UI from [uiState].
 * All user interactions are forwarded via the provided callbacks.
 *
 * When [uiState] is [AudioPlayerUiState.Loading] the full layout is still shown, but the
 * play/pause button displays a throbber and transport controls are disabled.
 *
 * The UI adapts based on [isPodcastMode]:
 * - **Podcast mode**: speed indicator, ±15 s seek, sleep timer.
 * - **Music mode**: shuffle, skip-prev/next, repeat.
 *
 * [isPodcastMode] is a separate state from [uiState] so that the correct mode can be shown
 * even while the player is still in the [AudioPlayerUiState.Loading] phase.
 *
 * In landscape orientation the track title (falling back to filename) and artist are shown in
 * the top-bar. The artwork either fills the entire screen as a blurred backdrop (when an
 * [AudioPlayerUiState.Data.artworkUri] is available) or the default audio icon is centred on
 * the existing gradient background.
 *
 * @param uiState Current UI state.
 * @param isPodcastMode Whether the podcast-mode control layout is active.
 * @param onPlayPauseClicked Called when the play/pause button is tapped.
 * @param onSeekTo Called with the target position in milliseconds when the user drags the slider.
 * @param onNextClicked Called when the skip-next button is tapped.
 * @param onPreviousClicked Called when the skip-previous button is tapped.
 * @param onShuffleClicked Called when the shuffle button is tapped (music mode only).
 * @param onRepeatClicked Called when the repeat button is tapped (music mode only).
 * @param onPlaylistClicked Called when the playlist button is tapped.
 * @param onBackPressed Called when the back button in the top bar is tapped.
 * @param onMoreActionsClicked Called when the more actions button in the top bar is tapped.
 * @param onToggleMode Called when the user taps the mode button to switch between podcast/music.
 * @param onSeekForward15 Called when the user taps the +15 s button (podcast mode only).
 * @param onSeekBackward15 Called when the user taps the −15 s button (podcast mode only).
 * @param onSpeedClicked Called when the user taps the speed indicator (podcast mode only).
 * @param onSleepTimerClicked Called when the user taps the sleep-timer button (podcast mode only).
 * @param sleepTimerState Current sleep timer state; drives the countdown label and icon indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    uiState: AudioPlayerUiState,
    isPodcastMode: Boolean,
    onPlayPauseClicked: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onRepeatClicked: () -> Unit,
    onPlaylistClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onMoreActionsClicked: () -> Unit,
    onToggleMode: () -> Unit,
    onSeekForward15: () -> Unit,
    onSeekBackward15: () -> Unit,
    onSpeedClicked: () -> Unit,
    onSleepTimerClicked: () -> Unit,
    modifier: Modifier = Modifier,
    sleepTimerState: SleepTimerState = SleepTimerState.Inactive,
) {
    AndroidTheme(isDark = true, useLegacyStatusBarColor = false) {
        // Must be the first child — see AudioPlayerWindowEffect KDoc for ordering requirements.
        AudioPlayerWindowEffect()
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true },
        ) {
            // Use actual layout dimensions so the detection works regardless of
            // Activity configChanges settings or multi-window/foldable scenarios.
            val isLandscape = maxWidth > maxHeight

            val topBarTitle: String
            val topBarSubtitle: String?
            if (isLandscape) {
                when (uiState) {
                    is AudioPlayerUiState.Loading -> {
                        topBarTitle = ""
                        topBarSubtitle = null
                    }

                    is AudioPlayerUiState.Data -> {
                        topBarTitle = uiState.title ?: uiState.currentPlayingItemName ?: ""
                        topBarSubtitle = uiState.artist
                    }
                }
            } else {
                topBarTitle = ""
                topBarSubtitle = null
            }

            MegaScaffoldWithTopAppBarScrollBehavior(
                topBar = {
                    AudioPlayerTopBar(
                        onBackPressed = onBackPressed,
                        onMoreActionsClicked = onMoreActionsClicked,
                        title = topBarTitle,
                        subtitle = topBarSubtitle,
                    )
                }
            ) { innerPadding ->
                val contentIsPlaying: Boolean
                val contentIsLoading: Boolean
                val contentPosition: Long
                val contentDuration: Long
                val contentTitle: String
                val contentArtist: String?
                val contentArtworkUri: String?
                val contentThumbnailData: ThumbnailData?
                val contentRepeatMode: Int
                val contentShuffleEnabled: Boolean
                val contentSpeed: Float

                when (uiState) {
                    is AudioPlayerUiState.Loading -> {
                        contentIsPlaying = false
                        contentIsLoading = true
                        contentPosition = 0L
                        contentDuration = 0L
                        contentTitle = ""
                        contentArtist = null
                        contentArtworkUri = null
                        contentThumbnailData = null
                        contentRepeatMode = Player.REPEAT_MODE_OFF
                        contentShuffleEnabled = false
                        contentSpeed = 1f
                    }

                    is AudioPlayerUiState.Data -> {
                        contentIsPlaying = uiState.isPlaying
                        contentIsLoading = uiState.isLoading
                        contentPosition = uiState.currentPosition
                        contentDuration = uiState.duration
                        contentTitle = uiState.title ?: uiState.currentPlayingItemName ?: ""
                        contentArtist = uiState.artist
                        contentArtworkUri = uiState.artworkUri
                        contentThumbnailData = uiState.thumbnailData
                        contentRepeatMode = uiState.repeatMode
                        contentShuffleEnabled = uiState.shuffleEnabled
                        contentSpeed = uiState.currentPlaybackSpeed
                    }
                }

                AudioPlayerContent(
                    isLandscape = isLandscape,
                    isPlaying = contentIsPlaying,
                    isLoading = contentIsLoading,
                    currentPosition = contentPosition,
                    duration = contentDuration,
                    title = contentTitle,
                    artist = contentArtist,
                    artworkUri = contentArtworkUri,
                    thumbnailData = contentThumbnailData,
                    repeatMode = contentRepeatMode,
                    shuffleEnabled = contentShuffleEnabled,
                    isPodcastMode = isPodcastMode,
                    currentPlaybackSpeed = contentSpeed,
                    sleepTimerState = sleepTimerState,
                    onPlayPauseClicked = onPlayPauseClicked,
                    onSeekTo = onSeekTo,
                    onNextClicked = onNextClicked,
                    onPreviousClicked = onPreviousClicked,
                    onShuffleClicked = onShuffleClicked,
                    onRepeatClicked = onRepeatClicked,
                    onPlaylistClicked = onPlaylistClicked,
                    contentPadding = innerPadding,
                    onToggleMode = onToggleMode,
                    onSeekForward15 = onSeekForward15,
                    onSeekBackward15 = onSeekBackward15,
                    onSpeedClicked = onSpeedClicked,
                    onSleepTimerClicked = onSleepTimerClicked,
                )
            }
        }
    }
}

@Composable
private fun AudioPlayerContent(
    isLandscape: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
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
    sleepTimerState: SleepTimerState,
    onPlayPauseClicked: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onRepeatClicked: () -> Unit,
    onPlaylistClicked: () -> Unit,
    contentPadding: PaddingValues,
    onToggleMode: () -> Unit,
    onSeekForward15: () -> Unit,
    onSeekBackward15: () -> Unit,
    onSpeedClicked: () -> Unit,
    onSleepTimerClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sleepTimerLabel = when (sleepTimerState) {
        is SleepTimerState.CountingDown -> stringResource(
            sharedR.string.audio_player_sleep_timer_countdown_label,
            formatCountdown(sleepTimerState.remaining),
        )

        SleepTimerState.EndOfTrack -> stringResource(sharedR.string.audio_player_sleep_timer_end_of_track)
        SleepTimerState.Inactive -> null
    }
    val sleepTimerActive = sleepTimerState !is SleepTimerState.Inactive
    val sleepTimerContentDescription = stringResource(sharedR.string.audio_player_sleep_timer_title)

    if (isLandscape) {
        AudioPlayerLandscapeContent(
            isPlaying = isPlaying,
            isLoading = isLoading,
            currentPosition = currentPosition,
            duration = duration,
            artworkUri = artworkUri,
            thumbnailData = thumbnailData,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled,
            isPodcastMode = isPodcastMode,
            currentPlaybackSpeed = currentPlaybackSpeed,
            sleepTimerLabel = sleepTimerLabel,
            sleepTimerActive = sleepTimerActive,
            sleepTimerContentDescription = sleepTimerContentDescription,
            onPlayPauseClicked = onPlayPauseClicked,
            onSeekTo = onSeekTo,
            onNextClicked = onNextClicked,
            onPreviousClicked = onPreviousClicked,
            onShuffleClicked = onShuffleClicked,
            onRepeatClicked = onRepeatClicked,
            onPlaylistClicked = onPlaylistClicked,
            contentPadding = contentPadding,
            onToggleMode = onToggleMode,
            onSeekForward15 = onSeekForward15,
            onSeekBackward15 = onSeekBackward15,
            onSpeedClicked = onSpeedClicked,
            onSleepTimerClicked = onSleepTimerClicked,
            modifier = modifier.testTag(AUDIO_PLAYER_CONTENT_TAG),
        )
    } else {
        AudioPlayerPortraitContent(
            isPlaying = isPlaying,
            isLoading = isLoading,
            currentPosition = currentPosition,
            duration = duration,
            title = title,
            artist = artist,
            artworkUri = artworkUri,
            thumbnailData = thumbnailData,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled,
            isPodcastMode = isPodcastMode,
            currentPlaybackSpeed = currentPlaybackSpeed,
            sleepTimerLabel = sleepTimerLabel,
            sleepTimerActive = sleepTimerActive,
            sleepTimerContentDescription = sleepTimerContentDescription,
            onPlayPauseClicked = onPlayPauseClicked,
            onSeekTo = onSeekTo,
            onNextClicked = onNextClicked,
            onPreviousClicked = onPreviousClicked,
            onShuffleClicked = onShuffleClicked,
            onRepeatClicked = onRepeatClicked,
            onPlaylistClicked = onPlaylistClicked,
            contentPadding = contentPadding,
            onToggleMode = onToggleMode,
            onSeekForward15 = onSeekForward15,
            onSeekBackward15 = onSeekBackward15,
            onSpeedClicked = onSpeedClicked,
            onSleepTimerClicked = onSleepTimerClicked,
            modifier = modifier.testTag(AUDIO_PLAYER_CONTENT_TAG),
        )
    }
}

@Preview
@Composable
private fun PreviewAudioPlayerScreenLoading() {
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

@Preview
@Composable
private fun PreviewAudioPlayerScreenPodcastMode() {
    AudioPlayerScreen(
        uiState = AudioPlayerUiState.Data(
            isPlaying = true,
            title = "Hardcore History: Supernova in the East",
            artist = "Dan Carlin",
            artworkUri = null,
            currentPosition = 97_000L,
            duration = 21_600_000L,
            shuffleEnabled = false,
            hasPlaylist = true,
            repeatMode = Player.REPEAT_MODE_OFF,
            isLoading = false,
            currentPlayingHandle = null,
            currentPlayingItemName = null,
            currentAdapterType = -1,
            thumbnailData = null,
            nodeSourceType = NodeSourceType.MEDIA_PLAYER_DEFAULT,
            fileLinkUrl = null,
            localFilePath = null,
            chatId = null,
            msgId = null,
            currentPlaybackSpeed = 1f,
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

@Preview
@Composable
private fun PreviewAudioPlayerScreenMusicMode() {
    AudioPlayerScreen(
        uiState = AudioPlayerUiState.Data(
            isPlaying = true,
            title = "Bohemian Rhapsody",
            artist = "Queen",
            artworkUri = null,
            currentPosition = 97_000L,
            duration = 354_000L,
            shuffleEnabled = true,
            hasPlaylist = true,
            repeatMode = Player.REPEAT_MODE_ONE,
            isLoading = false,
            currentPlayingHandle = null,
            currentPlayingItemName = null,
            currentAdapterType = -1,
            thumbnailData = null,
            nodeSourceType = NodeSourceType.MEDIA_PLAYER_DEFAULT,
            fileLinkUrl = null,
            localFilePath = null,
            chatId = null,
            msgId = null,
            currentPlaybackSpeed = 1f,
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

@Preview
@Composable
private fun PreviewAudioPlayerScreenPaused() {
    AudioPlayerScreen(
        uiState = AudioPlayerUiState.Data(
            isPlaying = false,
            title = null,
            artist = null,
            artworkUri = null,
            currentPosition = 0L,
            duration = 3_600_000L,
            repeatMode = Player.REPEAT_MODE_OFF,
            shuffleEnabled = false,
            isLoading = false,
            currentPlayingHandle = null,
            currentPlayingItemName = "podcast_episode_42.mp3",
            hasPlaylist = false,
            currentAdapterType = -1,
            thumbnailData = null,
            nodeSourceType = NodeSourceType.MEDIA_PLAYER_DEFAULT,
            fileLinkUrl = null,
            localFilePath = null,
            chatId = null,
            msgId = null,
            currentPlaybackSpeed = 1f,
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

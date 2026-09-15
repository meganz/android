package mega.privacy.android.feature.mediaplayer.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.Player
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.feature.mediaplayer.components.AudioPlayerGradientBackground
import mega.privacy.android.feature.mediaplayer.components.AudioPlayerLandscapeContent
import mega.privacy.android.feature.mediaplayer.components.AudioPlayerPortraitContent
import mega.privacy.android.feature.mediaplayer.components.AudioPlayerWindowEffect
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
 * @param onPlaylistClicked Called when the playlist button is tapped, only when no [queueContent]
 * is supplied to render the queue in place.
 * @param onQueueButtonPressed Called when the playlist button is tapped to open the queue (in
 * either mode); used for analytics only.
 * @param onBackPressed Called when the back button in the top bar is tapped.
 * @param onMoreActionsClicked Called when the more actions button in the top bar is tapped.
 * @param onToggleMode Called when the user taps the mode button to switch between podcast/music.
 * @param onSeekForward15 Called when the user taps the +15 s button (podcast mode only).
 * @param onSeekBackward15 Called when the user taps the −15 s button (podcast mode only).
 * @param onSpeedClicked Called when the user taps the speed indicator (podcast mode only).
 * @param onSleepTimerClicked Called when the user taps the sleep-timer button (podcast mode only).
 * @param sleepTimerState Current sleep timer state; drives the countdown label and icon indicator.
 * @param queueContent Optional play-queue list, rendered without a background or top bar of its
 * own. When provided, the playlist button reveals the queue instead of invoking
 * [onPlaylistClicked]: in landscape as a right-hand pane beside the player, which keeps its own
 * top bar, and in portrait as a full-screen overlay carrying a top bar of its own. The queue
 * stays visible across rotation, switching between the two forms.
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
    onBackPressed: () -> Unit,
    onMoreActionsClicked: () -> Unit,
    onToggleMode: () -> Unit,
    onSeekForward15: () -> Unit,
    onSeekBackward15: () -> Unit,
    onSpeedClicked: () -> Unit,
    onSleepTimerClicked: () -> Unit,
    modifier: Modifier = Modifier,
    sleepTimerState: SleepTimerState = SleepTimerState.Inactive,
    queueContent: (@Composable (Modifier) -> Unit)? = null,
    onPlaylistClicked: () -> Unit = {},
    onQueueButtonPressed: () -> Unit = {},
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

            var isQueueVisible by rememberSaveable { mutableStateOf(false) }
            val queueVisible = isQueueVisible && queueContent != null
            val showQueuePane = isLandscape && queueVisible
            val showQueueOverlay = !isLandscape && queueVisible
            BackHandler(enabled = queueVisible) { isQueueVisible = false }

            // Tracks the overlay for as long as it is on screen, including while it animates out.
            // Restoring the player's semantics on the dismiss tap instead would expose it to
            // accessibility services while the overlay still covers it and swallows taps.
            val overlayTransition = remember { MutableTransitionState(false) }
            overlayTransition.targetState = showQueueOverlay
            val overlayOnScreen =
                overlayTransition.currentState || overlayTransition.targetState

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

            val playerScaffold: @Composable (Modifier) -> Unit = { scaffoldModifier ->
                MegaScaffoldWithTopAppBarScrollBehavior(
                    // The portrait overlay covers the player completely; without this the player
                    // underneath stays reachable by accessibility services.
                    modifier = if (overlayOnScreen) {
                        scaffoldModifier.clearAndSetSemantics { }
                    } else {
                        scaffoldModifier
                    },
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
                    val contentControlsEnabled: Boolean
                    val contentNextEnabled: Boolean
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
                            contentControlsEnabled = false
                            contentNextEnabled = false
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
                            contentControlsEnabled = true
                            contentNextEnabled = uiState.hasNext
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
                        controlsEnabled = contentControlsEnabled,
                        nextEnabled = contentNextEnabled,
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
                        onPlaylistClicked = if (queueContent != null) {
                            {
                                if (!isQueueVisible) onQueueButtonPressed()
                                isQueueVisible = !isQueueVisible
                            }
                        } else {
                            {
                                onQueueButtonPressed()
                                onPlaylistClicked()
                            }
                        },
                        contentPadding = innerPadding,
                        onToggleMode = onToggleMode,
                        onSeekForward15 = onSeekForward15,
                        onSeekBackward15 = onSeekBackward15,
                        onSpeedClicked = onSpeedClicked,
                        onSleepTimerClicked = onSleepTimerClicked,
                    )
                }
            }

            // The player keeps a single call site across both layouts: moving it between branches
            // would dispose its subtree, resetting state such as whether the artwork has loaded.
            Row(modifier = Modifier.fillMaxSize()) {
                playerScaffold(
                    Modifier
                        .weight(1f)
                        // While the pane borders the end edge, the player must not reserve room
                        // for a navigation bar it does not touch.
                        .then(
                            if (showQueuePane) {
                                Modifier.consumeWindowInsets(
                                    WindowInsets.navigationBars.only(WindowInsetsSides.End)
                                )
                            } else {
                                Modifier
                            }
                        )
                )
                if (showQueuePane && queueContent != null) {
                    AudioPlayerQueuePane(
                        queueContent = queueContent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }

            AnimatedVisibility(
                visibleState = overlayTransition,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                if (queueContent != null) {
                    AudioPlayerQueueOverlay(
                        queueContent = queueContent,
                        onClose = { isQueueVisible = false },
                        onMoreActionsClicked = onMoreActionsClicked,
                    )
                }
            }
        }
    }
}

/**
 * Full-screen play-queue overlay for portrait.
 *
 * Unlike the landscape pane it covers the player, so it paints an opaque background — the gradient
 * colours alone are translucent — and carries its own top bar, whose back action closes the queue
 * rather than the player.
 *
 * Taps are swallowed at the root because the queue content does not claim pointer input across
 * the whole overlay — the loading placeholder is a plain box, and a short queue leaves the list
 * partly empty. Without this, such a tap would fall through to the player's transport controls
 * underneath. Child gestures still win, as they are dispatched before this handler.
 */
@Composable
private fun AudioPlayerQueueOverlay(
    queueContent: @Composable (Modifier) -> Unit,
    onClose: () -> Unit,
    onMoreActionsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AudioPlayerGradientBackground(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } },
        reversed = true,
        opaque = true,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AudioPlayerTopBar(
                onBackPressed = onClose,
                onMoreActionsClicked = onMoreActionsClicked,
            )
            queueContent(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

/**
 * Right-hand play-queue pane for landscape.
 *
 * Sits outside the player's scaffold: it has no top bar of its own, so it only needs to clear the
 * status bar rather than inherit the scaffold's top-bar inset.
 *
 * The navigation bar is inset on the end side only, which is the screen edge this pane occupies
 * in the row (right under LTR, left under RTL). When the bar is on the opposite edge — the device
 * rotated the other way — that side's inset is zero here and the player, which borders it, insets
 * it instead. The gradient still fills the full pane in every case.
 */
@Composable
private fun AudioPlayerQueuePane(
    queueContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    AudioPlayerGradientBackground(
        modifier = modifier,
        reversed = true,
    ) {
        queueContent(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.statusBars.union(
                        WindowInsets.navigationBars.only(WindowInsetsSides.End)
                    )
                ),
        )
    }
}

@Composable
private fun AudioPlayerContent(
    isLandscape: Boolean,
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
            controlsEnabled = controlsEnabled,
            nextEnabled = nextEnabled,
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
            controlsEnabled = controlsEnabled,
            nextEnabled = nextEnabled,
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
            hasNext = true,
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
            hasNext = true,
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
            hasNext = false,
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
private fun PreviewAudioPlayerQueueOverlay() {
    AndroidTheme(isDark = true, useLegacyStatusBarColor = false) {
        AudioPlayerQueueOverlay(
            queueContent = { queueModifier -> Box(modifier = queueModifier) },
            onClose = {},
            onMoreActionsClicked = {},
        )
    }
}

@Preview(widthDp = 400, heightDp = 400, name = "Queue pane – landscape half")
@Composable
private fun PreviewAudioPlayerQueuePane() {
    AndroidTheme(isDark = true, useLegacyStatusBarColor = false) {
        AudioPlayerQueuePane(
            queueContent = { queueModifier -> Box(modifier = queueModifier) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

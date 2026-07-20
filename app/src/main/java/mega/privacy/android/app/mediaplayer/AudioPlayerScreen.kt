package mega.privacy.android.app.mediaplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.MegaTextWithIndicator
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.image.MegaIconWithIndicator
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.mediaplayer.model.AudioPlayerUiState
import mega.privacy.android.app.presentation.videoplayer.view.DarkStatusBarEffect
import mega.privacy.android.app.presentation.videoplayer.view.TransparentNavigationBarEffect
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
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
 * - **Podcast mode**: shuffle, skip-prev/next, repeat.
 * - **Music mode**: speed indicator, ±15 s seek, sleep timer.
 *
 * [isPodcastMode] is a separate state from [uiState] so that the correct mode can be shown
 * even while the player is still in the [AudioPlayerUiState.Loading] phase.
 *
 * @param uiState Current UI state.
 * @param isPodcastMode Whether the podcast-mode control layout is active.
 * @param onPlayPauseClicked Called when the play/pause button is tapped.
 * @param onSeekTo Called with the target position in milliseconds when the user drags the slider.
 * @param onNextClicked Called when the skip-next button is tapped.
 * @param onPreviousClicked Called when the skip-previous button is tapped.
 * @param onShuffleClicked Called when the shuffle button is tapped (podcast mode only).
 * @param onRepeatClicked Called when the repeat button is tapped (podcast mode only).
 * @param onPlaylistClicked Called when the playlist button is tapped.
 * @param onBackPressed Called when the back button in the top bar is tapped.
 * @param onMoreActionsClicked Called when the more actions button in the top bar is tapped.
 * @param onToggleMode Called when the user taps the mode button to switch between podcast/music.
 * @param onSeekForward15 Called when the user taps the +15 s button (music mode only).
 * @param onSeekBackward15 Called when the user taps the −15 s button (music mode only).
 * @param onSpeedClicked Called when the user taps the speed indicator (music mode only).
 * @param onSleepTimerClicked Called when the user taps the sleep-timer button (music mode only).
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
) {
    TransparentNavigationBarEffect()
    DarkStatusBarEffect()
    OriginalTheme(isDark = true) {
        MegaScaffoldWithTopAppBarScrollBehavior(
            modifier = modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true },
            topBar = {
                AudioPlayerTopBar(
                    onBackPressed = onBackPressed,
                    onMoreActionsClicked = onMoreActionsClicked,
                )
            }
        ) { innerPadding ->
            when (uiState) {
                is AudioPlayerUiState.Loading -> AudioPlayerContent(
                    isPlaying = false,
                    isLoading = true,
                    currentPosition = 0L,
                    duration = 0L,
                    title = "",
                    artist = null,
                    artworkUri = null,
                    thumbnailData = null,
                    repeatMode = Player.REPEAT_MODE_OFF,
                    shuffleEnabled = false,
                    isPodcastMode = isPodcastMode,
                    currentPlaybackSpeed = 1f,
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

                is AudioPlayerUiState.Data -> AudioPlayerContent(
                    isPlaying = uiState.isPlaying,
                    isLoading = uiState.isLoading,
                    currentPosition = uiState.currentPosition,
                    duration = uiState.duration,
                    title = uiState.title ?: uiState.currentPlayingItemName ?: "",
                    artist = uiState.artist,
                    artworkUri = uiState.artworkUri,
                    thumbnailData = uiState.thumbnailData,
                    repeatMode = uiState.repeatMode,
                    shuffleEnabled = uiState.shuffleEnabled,
                    isPodcastMode = isPodcastMode,
                    currentPlaybackSpeed = uiState.currentPlaybackSpeed,
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
    onPlayPauseClicked: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onRepeatClicked: () -> Unit,
    onPlaylistClicked: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    onToggleMode: () -> Unit,
    onSeekForward15: () -> Unit,
    onSeekBackward15: () -> Unit,
    onSpeedClicked: () -> Unit,
    onSleepTimerClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AudioPlayerGradientTop,
                        AudioPlayerGradientBottom,
                    )
                )
            )
            .padding(top = contentPadding.calculateTopPadding(), start = 16.dp, end = 16.dp)
            .navigationBarsPadding()
            .testTag(AUDIO_PLAYER_CONTENT_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        ArtworkSection(
            artworkUri = artworkUri,
            thumbnailData = thumbnailData,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        MetadataSection(
            title = title,
            artist = artist,
        )

        Spacer(modifier = Modifier.height(24.dp))

        SeekBarSection(
            currentPosition = currentPosition,
            duration = duration,
            onSeekTo = onSeekTo,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isPodcastMode) {
            PodcastPlaybackControlsRow(
                isPlaying = isPlaying,
                isLoading = isLoading,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                onShuffleClicked = onShuffleClicked,
                onPreviousClicked = onPreviousClicked,
                onPlayPauseClicked = onPlayPauseClicked,
                onNextClicked = onNextClicked,
                onRepeatClicked = onRepeatClicked,
            )
        } else {
            MusicPlaybackControlsRow(
                isPlaying = isPlaying,
                isLoading = isLoading,
                currentPlaybackSpeed = currentPlaybackSpeed,
                onSpeedClicked = onSpeedClicked,
                onSeekBackward15 = onSeekBackward15,
                onPlayPauseClicked = onPlayPauseClicked,
                onSeekForward15 = onSeekForward15,
                onSleepTimerClicked = onSleepTimerClicked,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryActionRow(
            isPodcastMode = isPodcastMode,
            onAirplay = {},
            onToggleMode = onToggleMode,
            onPlaylistClicked = onPlaylistClicked,
        )
    }
}

@Composable
private fun ArtworkSection(
    artworkUri: String?,
    thumbnailData: ThumbnailData?,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        if (artworkUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
            )
        } else {
            NodeThumbnailView(
                data = thumbnailData,
                contentDescription = null,
                defaultImage = iconPackR.drawable.ic_audio_medium_solid,
                layoutType = ThumbnailLayoutType.FullSize,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .fillMaxSize(),
            )
        }
    }
}

@Composable
private fun MetadataSection(
    title: String,
    artist: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        MegaText(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textColor = TextColor.Primary
        )
        if (!artist.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            MegaText(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                textColor = TextColor.Secondary
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun SeekBarSection(
    currentPosition: Long,
    duration: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableLongStateOf(0L) }
    val currentOnSeekTo by rememberUpdatedState(onSeekTo)

    val displayPosition = if (isScrubbing) scrubPosition else currentPosition

    Column(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            factory = { context ->
                DefaultTimeBar(context).apply {
                    setPlayedColor(AudioPlayerBrandColorHex.toColorInt())
                    setScrubberColor(AudioPlayerBrandColorHex.toColorInt())
                    addListener(object : TimeBar.OnScrubListener {
                        override fun onScrubStart(timeBar: TimeBar, position: Long) {
                            isScrubbing = true
                            scrubPosition = position
                        }

                        override fun onScrubMove(timeBar: TimeBar, position: Long) {
                            scrubPosition = position
                        }

                        override fun onScrubStop(
                            timeBar: TimeBar,
                            position: Long,
                            canceled: Boolean,
                        ) {
                            isScrubbing = false
                            if (!canceled) currentOnSeekTo(position)
                        }
                    })
                }
            },
            update = { timeBar ->
                timeBar.setDuration(duration)
                if (!isScrubbing) {
                    timeBar.setPosition(currentPosition)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MegaText(
                text = formatMs(displayPosition),
                style = MaterialTheme.typography.bodySmall,
                textColor = TextColor.Secondary,
            )
            MegaText(
                text = formatMs(duration),
                style = MaterialTheme.typography.bodySmall,
                textColor = TextColor.Secondary,
            )
        }
    }
}

@Composable
private fun PodcastPlaybackControlsRow(
    isPlaying: Boolean,
    isLoading: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onShuffleClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onRepeatClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE)
        IconPack.Medium.Regular.Solid.RepeatOne
    else
        IconPack.Medium.Regular.Solid.Repeat
    val repeatActive = repeatMode != Player.REPEAT_MODE_OFF

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onShuffleClicked) {
            MegaIconWithIndicator(
                imageVector = IconPack.Medium.Regular.Solid.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) IconColor.Brand else IconColor.Primary,
                showIndicator = shuffleEnabled
            )
        }

        IconButton(
            onClick = onPreviousClicked,
            enabled = !isLoading,
        ) {
            MegaIcon(
                imageVector = IconPack.Medium.Regular.Solid.SkipBack,
                tint = IconColor.Primary,
                contentDescription = "Previous",
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(
            onClick = onPlayPauseClicked,
            enabled = !isLoading,
            modifier = Modifier.size(64.dp),
        ) {
            if (isLoading) {
                MediaPlayerLoadingIndicator(modifier = Modifier.size(64.dp))
            } else {
                MegaIcon(
                    imageVector =
                        if (isPlaying) IconPack.Medium.Regular.Solid.Pause
                        else IconPack.Medium.Regular.Solid.Play,
                    tint = IconColor.Primary,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(64.dp),
                )
            }
        }

        IconButton(
            onClick = onNextClicked,
            enabled = !isLoading,
        ) {
            MegaIcon(
                imageVector = IconPack.Medium.Regular.Solid.SkipForward,
                tint = IconColor.Primary,
                contentDescription = "Next",
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(onClick = onRepeatClicked) {
            MegaIconWithIndicator(
                imageVector = repeatIcon,
                contentDescription = "Repeat",
                tint = if (repeatActive) IconColor.Brand else IconColor.Primary,
                showIndicator = repeatActive
            )
        }
    }
}

@Composable
private fun MusicPlaybackControlsRow(
    isPlaying: Boolean,
    isLoading: Boolean,
    currentPlaybackSpeed: Float,
    onSpeedClicked: () -> Unit,
    onSeekBackward15: () -> Unit,
    onPlayPauseClicked: () -> Unit,
    onSeekForward15: () -> Unit,
    onSleepTimerClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val speedNotDefault = remember(currentPlaybackSpeed) { currentPlaybackSpeed != 1f }
    val speedText = remember(currentPlaybackSpeed) {
        if (currentPlaybackSpeed % 1f == 0f) "${currentPlaybackSpeed.toInt()}x"
        else "${currentPlaybackSpeed}x"
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clickable(onClick = onSpeedClicked),
        ) {
            MegaTextWithIndicator(
                text = speedText,
                style = MaterialTheme.typography.titleMedium,
                textColor = if (speedNotDefault) TextColor.Brand else TextColor.Primary,
                showIndicator = speedNotDefault,
            )
        }

        IconButton(
            onClick = onSeekBackward15,
            enabled = !isLoading,
        ) {
            MegaIcon(
                imageVector = IconPack.Medium.Regular.Outline.FifteenBackward,
                tint = IconColor.Primary,
                contentDescription = "Seek backward 15 seconds",
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(
            onClick = onPlayPauseClicked,
            enabled = !isLoading,
            modifier = Modifier.size(64.dp),
        ) {
            if (isLoading) {
                MediaPlayerLoadingIndicator(modifier = Modifier.size(64.dp))
            } else {
                MegaIcon(
                    imageVector =
                        if (isPlaying) IconPack.Medium.Regular.Solid.Pause
                        else IconPack.Medium.Regular.Solid.Play,
                    tint = IconColor.Primary,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(64.dp),
                )
            }
        }

        IconButton(
            onClick = onSeekForward15,
            enabled = !isLoading,
        ) {
            MegaIcon(
                imageVector = IconPack.Medium.Regular.Outline.FifteenForward,
                tint = IconColor.Primary,
                contentDescription = "Seek forward 15 seconds",
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(onClick = onSleepTimerClicked) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.ClockStopwatchShort,
                tint = IconColor.Primary,
                contentDescription = "Sleep timer",
            )
        }
    }
}

@Composable
private fun PrimaryActionRow(
    isPodcastMode: Boolean,
    onAirplay: () -> Unit,
    onToggleMode: () -> Unit,
    onPlaylistClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onAirplay) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.Airplay,
                tint = IconColor.Primary,
                contentDescription = "Airplay",
            )
        }

        SecondaryFilledButton(
            text = stringResource(
                if (isPodcastMode) sharedR.string.audio_player_podcast_mode_label
                else sharedR.string.audio_player_music_mode_label
            ),
            onClick = onToggleMode,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )

        IconButton(onClick = onPlaylistClicked) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.Playlist,
                tint = IconColor.Primary,
                contentDescription = "Playlist",
            )
        }
    }
}

private val AudioPlayerGradientTop = Color(91, 19, 13)
private val AudioPlayerGradientBottom = Color(21, 22, 22)
private const val AudioPlayerBrandColorHex = "#F23433"

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0)
        "%d:%02d:%02d".format(hours, minutes, seconds)
    else
        "%d:%02d".format(minutes, seconds)
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

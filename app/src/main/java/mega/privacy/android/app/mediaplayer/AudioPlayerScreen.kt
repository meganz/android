package mega.privacy.android.app.mediaplayer

import androidx.annotation.OptIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.image.MegaIconWithIndicator
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.mediaplayer.model.AudioPlayerUiState
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

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
 * @param uiState Current UI state.
 * @param onPlayPauseClicked Called when the play/pause button is tapped.
 * @param onSeekTo Called with the target position in milliseconds when the user drags the slider.
 * @param onNextClicked Called when the skip-next button is tapped.
 * @param onPreviousClicked Called when the skip-previous button is tapped.
 * @param onShuffleClicked Called when the shuffle button is tapped.
 * @param onRepeatClicked Called when the repeat button is tapped.
 * @param onPlaylistClicked Called when the playlist button is tapped.
 * @param onScreenClicked Called when the screen background is tapped (toggles toolbar).
 */
@Composable
fun AudioPlayerScreen(
    uiState: AudioPlayerUiState,
    onPlayPauseClicked: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onRepeatClicked: () -> Unit,
    onPlaylistClicked: () -> Unit,
    onScreenClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OriginalTheme(isDark = true) {
        Box(
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
                .clickable(onClick = onScreenClicked)
        ) {
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
                    onPlayPauseClicked = onPlayPauseClicked,
                    onSeekTo = onSeekTo,
                    onNextClicked = onNextClicked,
                    onPreviousClicked = onPreviousClicked,
                    onShuffleClicked = onShuffleClicked,
                    onRepeatClicked = onRepeatClicked,
                    onPlaylistClicked = onPlaylistClicked,
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
                    onPlayPauseClicked = onPlayPauseClicked,
                    onSeekTo = onSeekTo,
                    onNextClicked = onNextClicked,
                    onPreviousClicked = onPreviousClicked,
                    onShuffleClicked = onShuffleClicked,
                    onRepeatClicked = onRepeatClicked,
                    onPlaylistClicked = onPlaylistClicked,
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
    onPlayPauseClicked: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onRepeatClicked: () -> Unit,
    onPlaylistClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
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

        PlaybackControlsRow(
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

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryActionRow(
            onAirplay = {},
            onActionClicked = {},
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

@OptIn(UnstableApi::class)
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
private fun PlaybackControlsRow(
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
    val infiniteTransition = rememberInfiniteTransition(label = "loader")
    val loaderRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "loaderRotation",
    )
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
            MegaIcon(
                imageVector = when {
                    isLoading -> IconPack.Medium.Thin.Outline.LoaderThrobber
                    isPlaying -> IconPack.Medium.Regular.Solid.Pause
                    else -> IconPack.Medium.Regular.Solid.Play
                },
                tint = IconColor.Primary,
                contentDescription = when {
                    isLoading -> "Loading"
                    isPlaying -> "Pause"
                    else -> "Play"
                },
                modifier = Modifier
                    .size(64.dp)
                    .then(if (isLoading) Modifier.rotate(loaderRotation) else Modifier),
            )
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
private fun PrimaryActionRow(
    onAirplay: () -> Unit,
    onActionClicked: () -> Unit,
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
            text = "Podcast mode",
            onClick = onActionClicked,
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
        onPlayPauseClicked = {},
        onSeekTo = {},
        onNextClicked = {},
        onPreviousClicked = {},
        onShuffleClicked = {},
        onRepeatClicked = {},
        onPlaylistClicked = {},
        onScreenClicked = {},
    )
}

@Preview
@Composable
private fun PreviewAudioPlayerScreenPlaying() {
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
            currentPlayingHandle = -1L,
            currentPlayingItemName = null,
            currentAdapterType = -1,
            thumbnailData = null,
        ),
        onPlayPauseClicked = {},
        onSeekTo = {},
        onNextClicked = {},
        onPreviousClicked = {},
        onShuffleClicked = {},
        onRepeatClicked = {},
        onPlaylistClicked = {},
        onScreenClicked = {},
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
            currentPlayingHandle = -1L,
            currentPlayingItemName = "podcast_episode_42.mp3",
            hasPlaylist = false,
            currentAdapterType = -1,
            thumbnailData = null,
        ),
        onPlayPauseClicked = {},
        onSeekTo = {},
        onNextClicked = {},
        onPreviousClicked = {},
        onShuffleClicked = {},
        onRepeatClicked = {},
        onPlaylistClicked = {},
        onScreenClicked = {},
    )
}

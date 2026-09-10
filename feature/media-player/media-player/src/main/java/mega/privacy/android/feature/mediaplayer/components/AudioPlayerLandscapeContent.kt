package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.icon.pack.R as iconPackR

/**
 * Landscape layout for the audio player.
 *
 * When [artworkUri] or [thumbnailData] is available the image fills the entire screen as a
 * backdrop with a dark gradient overlay so the controls remain readable ([artworkUri] takes
 * priority). When neither is available the standard gradient background is used and the default
 * audio icon is centred above the controls.
 *
 * The landscape layout differs from portrait in that playback controls are vertically centred
 * in the available space while the seek bar and primary actions are pinned at the bottom.
 * This means it composes the playback controls and seek bar separately rather than using the
 * [AudioPlayerController] composite.
 */
@Composable
fun AudioPlayerLandscapeContent(
    isPlaying: Boolean,
    isLoading: Boolean,
    controlsEnabled: Boolean,
    nextEnabled: Boolean,
    currentPosition: Long,
    duration: Long,
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
    // True only after the background image (artworkUri or thumbnailData) actually loads.
    // Starts false so the default icon shows immediately; hides when the image succeeds.
    var backgroundLoaded by remember(artworkUri, thumbnailData) { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        AudioPlayerGradientBackground(modifier = Modifier.fillMaxSize())

        if (artworkUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { backgroundLoaded = true },
                onError = { backgroundLoaded = false },
            )
        } else if (thumbnailData != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailData)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { backgroundLoaded = true },
                onError = { backgroundLoaded = false },
            )
        }

        if (backgroundLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.75f),
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    start = 16.dp,
                    end = 16.dp,
                )
                .navigationBarsPadding(),
        ) {
            // The default audio icon lives here too so it shares the exact same center
            // as the controls, keeping both aligned regardless of navigation bar insets.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (!backgroundLoaded) {
                    Image(
                        painter = painterResource(iconPackR.drawable.ic_audio_medium_solid),
                        contentDescription = null,
                        modifier = Modifier.size(160.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
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
                        previousEnabled = controlsEnabled,
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
            }

            AudioPlayerSeekBar(
                currentPosition = currentPosition,
                duration = duration,
                onSeekTo = onSeekTo,
                sleepTimerLabel = sleepTimerLabel,
            )

            Spacer(modifier = Modifier.height(5.dp))

            AudioPlayerPrimaryActions(
                isPodcastMode = isPodcastMode,
                onToggleMode = onToggleMode,
                onPlaylistClicked = onPlaylistClicked,
            )
        }
    }
}

@Preview(widthDp = 800, heightDp = 400, name = "Landscape – no artwork")
@Composable
private fun PreviewAudioPlayerLandscapeContent() {
    AndroidTheme(isDark = true) {
        AudioPlayerLandscapeContent(
            isPlaying = false,
            isLoading = false,
            controlsEnabled = true,
            nextEnabled = true,
            currentPosition = 45_000L,
            duration = 240_000L,
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

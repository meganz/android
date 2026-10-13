package mega.privacy.android.feature.mediaplayer.presentation.miniplayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

// Matches R.dimen.audio_player_mini_controller_height, the height the PlayerView bar reserves in
// every host; the resource lives in :app and cannot be read from here.
private val MiniPlayerHeight = 64.dp
private val PlayPauseTouchTarget = 40.dp
private val PlayPauseIconSize = 24.dp
private val LoadingIndicatorSize = 32.dp

// How far the bar has to travel before a drag counts as a track change. The design note asks
// for the gesture but does not give a distance; this is about a thumb's width, far enough not
// to fire while the user is really trying to tap the bar open.
private val SwipeThreshold = 72.dp

// Long enough to read as a deliberate slide, short enough that the next title is in place
// before the finger has left the bar. Not from the design note, which only asks for the gesture.
private const val TrackChangeDurationMs = 300

// Speed, in dp per second, above which a flick counts even when it did not travel the full
// threshold. `onDragStopped` reports pixels per second, so this is converted with the density.
// Also not from the design note; it is roughly a deliberate flick rather than a slow drag.
private val FlickVelocityPerSecond = 400.dp

/**
 * Compose mini player for the revamped audio player.
 *
 * Stateless: the host owns [state] and every action is a lambda. Replaces the Media3 `PlayerView`
 * in `mini_audio_player.xml`, which is shared with the legacy player and therefore cannot be
 * changed for V2 alone.
 *
 * A host renders this only while both audio player flags are on; the XML bar stays in place to
 * serve legacy sessions.
 *
 * @param state What to render.
 * @param onPlayPauseClicked Called when the play/pause button is tapped.
 * @param onCloseClicked Called when the close button is tapped.
 * @param onPlayerClicked Called when the bar itself is tapped, to open the full-screen player.
 * @param onSwipeToNext Called when the bar is swiped towards the start edge.
 * @param onSwipeToPrevious Called when the bar is swiped towards the end edge.
 * @param modifier Modifier.
 */
@Composable
internal fun AudioMiniPlayer(
    state: AudioMiniPlayerUiState.Data,
    onPlayPauseClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    onPlayerClicked: () -> Unit,
    onSwipeToNext: () -> Unit,
    onSwipeToPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    // A second swipe can start while the first is still easing back. The flag stops the settle
    // animation writing absolute values over the new drag, and the baseline keeps the threshold
    // measuring this drag alone rather than whatever the previous one left on screen.
    var isDragging by remember { mutableStateOf(false) }
    var dragStartOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { SwipeThreshold.toPx() }
    val flickVelocityPx = with(density) { FlickVelocityPerSecond.toPx() }

    // Which way the titles travel on the next change. A swipe sets it so they follow the finger;
    // it then returns to forward, which is where a track that simply ended is going. Starts at
    // zero, which leaves the first title to appear without sliding.
    var swipeDirection by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.title, state.artist) {
        // Runs after the transition for this change has read the direction.
        swipeDirection = 1
    }

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
                .fillMaxHeight()
                .draggable(
                    state = rememberDraggableState { delta -> swipeOffset += delta },
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        isDragging = true
                        dragStartOffset = swipeOffset
                    },
                    onDragStopped = { velocity ->
                        isDragging = false
                        // A short flick names a track as clearly as a long drag, so either the
                        // distance or the speed can carry the gesture. Distance is read first:
                        // a drag that already went far enough should not be reversed by the
                        // direction the finger happened to be moving as it left the screen.
                        val travelled = swipeOffset - dragStartOffset
                        val direction = when {
                            travelled <= -thresholdPx -> 1
                            travelled >= thresholdPx -> -1
                            velocity <= -flickVelocityPx -> 1
                            velocity >= flickVelocityPx -> -1
                            else -> 0
                        }
                        if (direction != 0) {
                            swipeDirection = direction
                            if (direction == 1) onSwipeToNext() else onSwipeToPrevious()
                        }
                        animate(swipeOffset, 0f) { value, _ ->
                            if (!isDragging) swipeOffset = value
                        }
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayPauseControl(
                isPlaying = state.isPlaying,
                isLoading = state.isLoading,
                onClick = onPlayPauseClicked,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            AnimatedContent(
                targetState = state.title to state.artist,
                transitionSpec = {
                    // One spec for both halves: at any point the outgoing title sits at -p×width
                    // and the incoming one at (1-p)×width, so their edges stay together and the
                    // pair reads as one strip rather than two titles crossing over.
                    val direction = swipeDirection
                    val spec = tween<IntOffset>(TrackChangeDurationMs)
                    slideInHorizontally(spec) { width -> direction * width }
                        .togetherWith(slideOutHorizontally(spec) { width -> -direction * width })
                },
                label = "miniPlayerTrack",
                modifier = Modifier
                    .weight(1f)
                    .offset { IntOffset(swipeOffset.roundToInt(), 0) },
            ) { (title, artist) ->
                Column {
                    MegaText(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        textColor = TextColor.Primary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                    )
                    if (!artist.isNullOrBlank()) {
                        MegaText(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall,
                            textColor = TextColor.Secondary,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                        )
                    }
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
            onSwipeToNext = {},
            onSwipeToPrevious = {},
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
            onSwipeToNext = {},
            onSwipeToPrevious = {},
        )
    }
}

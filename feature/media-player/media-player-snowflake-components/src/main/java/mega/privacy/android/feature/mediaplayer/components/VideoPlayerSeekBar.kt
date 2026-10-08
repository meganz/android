package mega.privacy.android.feature.mediaplayer.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import java.util.Formatter
import java.util.Locale

/**
 * Elapsed time, total time and the scrub bar for the video player, laid out as the live player
 * has them: the pair of times above the bar, separated by a slash.
 *
 * A Media3 [DefaultTimeBar] in an `AndroidView` rather than a Compose slider, matching
 * [AudioPlayerSeekBar]: it already implements the touch target, the drag threshold and the
 * scrubber growth a slider would have to reproduce, and it is what the live player uses, so the
 * feel is identical rather than merely similar.
 *
 * Separate from [AudioPlayerSeekBar] rather than a parameter on it: audio puts the two times
 * below the bar and apart, video puts them above it and together, and audio's bar sits on a
 * surface that follows the theme while this one sits on the picture, which is dark whatever
 * theme the app is in. The colours and the text style differ for that reason.
 *
 * @param currentPositionMs where playback has reached.
 * @param durationMs length of the item, `0` until the player knows it.
 * @param onSeekTo called once the drag ends, never while it is in progress: seeking on every
 * touch move would make the player decode its way through everything the finger passed over.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerSeekBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }

    // The listener is installed once, in the factory, so without this it would capture the first
    // callback it was given and keep calling that one after a recomposition replaced it.
    val currentOnSeekTo by rememberUpdatedState(onSeekTo)

    // While dragging, the time has to follow the finger rather than the player, which has not
    // moved yet — the seek is only issued when the drag ends.
    val displayedPositionMs = if (isScrubbing) scrubPositionMs else currentPositionMs

    // Follows the theme, as the live player's `played_color`/`scrubber_color` do: both are
    // `@color/color_button_brand`, which has a night variant. The two greys below do not.
    val playedColor = DSTokens.colors.brand.default
    val playedColorArgb = remember(playedColor) { playedColor.toArgb() }

    val formatBuilder = remember { StringBuilder() }
    val formatter = remember { Formatter(formatBuilder, Locale.getDefault()) }

    Column(modifier = modifier.fillMaxWidth()) {
        MegaText(
            text = "${Util.getStringForTime(formatBuilder, formatter, displayedPositionMs)}" +
                    " / ${Util.getStringForTime(formatBuilder, formatter, durationMs)}",
            style = MaterialTheme.typography.bodyMedium,
            textColor = TextColor.OnColor,
            modifier = Modifier
                .padding(start = 16.dp, bottom = 10.dp)
                .testTag(VIDEO_PLAYER_TIME_TEST_TAG),
        )
        AndroidView(
            factory = { context ->
                DefaultTimeBar(context).apply {
                    setBufferedColor(BUFFERED_COLOR)
                    setUnplayedColor(UNPLAYED_COLOR)
                    addListener(object : TimeBar.OnScrubListener {
                        override fun onScrubStart(timeBar: TimeBar, position: Long) {
                            isScrubbing = true
                            scrubPositionMs = position
                        }

                        override fun onScrubMove(timeBar: TimeBar, position: Long) {
                            scrubPositionMs = position
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
                // Set here rather than in the factory, which runs once: these two follow the
                // theme, and an in-app theme switch recomposes without recreating the view.
                timeBar.setPlayedColor(playedColorArgb)
                timeBar.setScrubberColor(playedColorArgb)
                timeBar.setDuration(durationMs)
                // Left alone mid-drag: the bar is already showing the finger's position, and
                // writing the player's stale one back would fight it.
                if (!isScrubbing) timeBar.setPosition(currentPositionMs)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SEEK_BAR_HORIZONTAL_PADDING)
                .testTag(VIDEO_PLAYER_SEEK_BAR_TEST_TAG),
        )
    }
}

/**
 * 6dp, which together with half of the dragged scrubber puts the bar's ends on the same 16dp
 * optical margin as the text above it. Carried over from `video_player_control_revamp.xml`,
 * where the same figure is spelled out for the same reason.
 */
private val SEEK_BAR_HORIZONTAL_PADDING = 6.dp

/** `grey_300` in the live player's controller layout. Fixed, because the bar is on the picture. */
private const val BUFFERED_COLOR = 0xFF9E9E9E.toInt()

/** `grey_600` in the live player's controller layout. */
private const val UNPLAYED_COLOR = 0xFF6C6C6C.toInt()

/** Elapsed and total time, as one "00:12 / 03:45" label. */
const val VIDEO_PLAYER_TIME_TEST_TAG = "video_player:time"

/** The scrub bar. */
const val VIDEO_PLAYER_SEEK_BAR_TEST_TAG = "video_player:seek_bar"

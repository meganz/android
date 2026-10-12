package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.icon.pack.IconPack

/**
 * The row of transport controls across the middle of the picture, laid out as the live player
 * has it: rewind and fast-forward on the outside, previous and next inside them, play/pause in
 * the centre.
 *
 * The order is the live player's and is worth noting because it is the opposite of the usual —
 * `exo_rew` is pinned to the start edge and `exo_prev` sits between it and the button.
 *
 * `SpaceBetween` places the middle child exactly on centre because the pairs either side are the
 * same width as each other, which is what the live layout achieves by constraining play/pause to
 * the parent and chaining the rest off it.
 */
@Composable
internal fun VideoPlayerTransportControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    onPreviousClick: () -> Unit,
    onRewindClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onFastForwardClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = ROW_HORIZONTAL_MARGIN),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportButton(
            icon = IconPack.Medium.Regular.Outline.FifteenBackward,
            contentDescription = "Rewind $SEEK_SECONDS seconds",
            onClick = onRewindClick,
            testTag = VIDEO_PLAYER_REWIND_TEST_TAG,
        )
        TransportButton(
            icon = IconPack.Medium.Regular.Outline.SkipBack,
            contentDescription = "Previous",
            onClick = onPreviousClick,
            testTag = VIDEO_PLAYER_PREVIOUS_TEST_TAG,
        )
        VideoPlayerPlayPauseButton(
            isPlaying = isPlaying,
            isLoading = isLoading,
            onClick = onPlayPauseClick,
            modifier = Modifier.testTag(VIDEO_PLAYER_PLAY_PAUSE_TEST_TAG),
        )
        TransportButton(
            icon = IconPack.Medium.Regular.Outline.SkipForward,
            contentDescription = "Next",
            onClick = onNextClick,
            testTag = VIDEO_PLAYER_NEXT_TEST_TAG,
        )
        TransportButton(
            icon = IconPack.Medium.Regular.Outline.FifteenForward,
            contentDescription = "Fast forward $SEEK_SECONDS seconds",
            onClick = onFastForwardClick,
            testTag = VIDEO_PLAYER_FAST_FORWARD_TEST_TAG,
        )
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(BUTTON_SIZE).testTag(testTag),
    ) {
        MegaIcon(
            imageVector = icon,
            tint = IconColor.OnColor,
            contentDescription = contentDescription,
            modifier = Modifier.size(ICON_SIZE),
        )
    }
}

/**
 * The fifteen the icons draw and the labels say is the increment `VideoPlayerService` builds the
 * player with (`SEEK_INCREMENT`). Nothing couples them, so a change there has to be mirrored in
 * the two icons and the two content descriptions above.
 */
private const val SEEK_SECONDS = 15

/** What the live player keeps between the outermost buttons and the screen edges. */
private val ROW_HORIZONTAL_MARGIN = 24.dp

/** `AudioPlayerButton`, the style the live player gives all four of these. */
private val BUTTON_SIZE = 52.dp

/**
 * What the live player's four transport vectors declare — `ic_player_prev` and the rest are
 * 24dp wide inside the 52dp touch target, and `ImageButton` draws them at that size rather than
 * filling the button.
 */
private val ICON_SIZE = 24.dp

/** Rewind fifteen seconds. */
const val VIDEO_PLAYER_REWIND_TEST_TAG = "video_player:rewind"

/** Previous item, or a restart of the current one. */
const val VIDEO_PLAYER_PREVIOUS_TEST_TAG = "video_player:previous"

/** Next item. */
const val VIDEO_PLAYER_NEXT_TEST_TAG = "video_player:next"

/** Fast forward fifteen seconds. */
const val VIDEO_PLAYER_FAST_FORWARD_TEST_TAG = "video_player:fast_forward"

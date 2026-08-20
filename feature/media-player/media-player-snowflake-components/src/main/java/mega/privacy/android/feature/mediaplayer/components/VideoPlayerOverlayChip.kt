package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as SharedR

/**
 * State types for the overlay chip shown above the video player controls.
 * Add a new subclass here to introduce additional chip variants.
 */
sealed class VideoPlayerOverlayChipState {
    /** Seek-gesture chip: shows elapsed seek seconds with a directional icon. */
    data class Seek(val seconds: Int, val isForward: Boolean) : VideoPlayerOverlayChipState()

    /** Long-press speed chip: shows the active speed text followed by a forward icon. */
    data class LongPressSpeedHeld(val speedText: String) : VideoPlayerOverlayChipState()

    /** Pinch-zoom chip: shows the boundary label or the current zoom percentage. */
    sealed class Zoom : VideoPlayerOverlayChipState() {
        data object FitToScreen : Zoom()
        data object FillScreen : Zoom()
        data class Percentage(val percent: Int) : Zoom()
    }
}

/**
 * Overlay chip rendered above the video player controls.
 * Renders nothing when [state] is null, so callers can pass null to hide the chip.
 */
@Composable
fun VideoPlayerOverlayChip(
    state: VideoPlayerOverlayChipState?,
) {
    state ?: return

    val rowModifier = Modifier
        .clip(RoundedCornerShape(16.dp))
        .background(DSTokens.colors.background.blur)
        .padding(horizontal = 12.dp, vertical = 6.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier,
    ) {
        when (state) {
            is VideoPlayerOverlayChipState.Seek -> SeekChipContent(state)
            is VideoPlayerOverlayChipState.LongPressSpeedHeld -> LongPressSpeedHeldChipContent(state)
            is VideoPlayerOverlayChipState.Zoom -> ZoomChipContent(state)
        }
    }
}

@Composable
private fun SeekChipContent(state: VideoPlayerOverlayChipState.Seek) {
    if (!state.isForward) {
        MegaIcon(
            painter = rememberVectorPainter(IconPack.Medium.Regular.Solid.FastBackward),
            contentDescription = null,
            tint = IconColor.Primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
    MegaText(
        text = pluralStringResource(
            SharedR.plurals.video_player_seek_seconds,
            state.seconds,
            state.seconds,
        ),
        textColor = TextColor.Primary,
        style = AppTheme.typography.labelLarge,
    )
    if (state.isForward) {
        Spacer(modifier = Modifier.width(8.dp))
        MegaIcon(
            painter = rememberVectorPainter(IconPack.Medium.Regular.Solid.FastForward),
            contentDescription = null,
            tint = IconColor.Primary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun LongPressSpeedHeldChipContent(state: VideoPlayerOverlayChipState.LongPressSpeedHeld) {
    MegaText(
        text = state.speedText,
        textColor = TextColor.Primary,
        style = AppTheme.typography.labelLarge,
    )
    Spacer(modifier = Modifier.width(8.dp))
    MegaIcon(
        painter = rememberVectorPainter(IconPack.Medium.Regular.Solid.FastForward),
        contentDescription = null,
        tint = IconColor.Primary,
        modifier = Modifier.size(16.dp),
    )
}

@Composable
private fun ZoomChipContent(state: VideoPlayerOverlayChipState.Zoom) {
    MegaText(
        text = when (state) {
            VideoPlayerOverlayChipState.Zoom.FitToScreen ->
                stringResource(SharedR.string.video_player_zoom_fit_to_screen)

            VideoPlayerOverlayChipState.Zoom.FillScreen ->
                stringResource(SharedR.string.video_player_zoom_fill_screen)

            is VideoPlayerOverlayChipState.Zoom.Percentage -> "${state.percent}%"
        },
        textColor = TextColor.Primary,
        style = AppTheme.typography.labelLarge,
    )
}

@Preview
@Composable
private fun SeekForwardChipPreview() {
    VideoPlayerOverlayChip(VideoPlayerOverlayChipState.Seek(seconds = 30, isForward = true))
}

@Preview
@Composable
private fun SeekBackwardChipPreview() {
    VideoPlayerOverlayChip(VideoPlayerOverlayChipState.Seek(seconds = 15, isForward = false))
}

@Preview
@Composable
private fun LongPressSpeedHeldChipPreview() {
    VideoPlayerOverlayChip(VideoPlayerOverlayChipState.LongPressSpeedHeld(speedText = "2×"))
}

@Preview
@Composable
private fun ZoomFitToScreenChipPreview() {
    VideoPlayerOverlayChip(VideoPlayerOverlayChipState.Zoom.FitToScreen)
}

@Preview
@Composable
private fun ZoomFillScreenChipPreview() {
    VideoPlayerOverlayChip(VideoPlayerOverlayChipState.Zoom.FillScreen)
}

@Preview
@Composable
private fun ZoomPercentageChipPreview() {
    VideoPlayerOverlayChip(VideoPlayerOverlayChipState.Zoom.Percentage(percent = 250))
}

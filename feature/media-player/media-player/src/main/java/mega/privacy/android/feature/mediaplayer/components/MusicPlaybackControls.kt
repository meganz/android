package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.image.MegaIconWithIndicator
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.icon.pack.IconPack

@Composable
internal fun MusicPlaybackControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    controlsEnabled: Boolean,
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
                showIndicator = shuffleEnabled,
            )
        }

        IconButton(
            onClick = onPreviousClicked,
            enabled = controlsEnabled,
        ) {
            MegaIcon(
                imageVector = IconPack.Medium.Regular.Solid.SkipBack,
                tint = IconColor.Primary,
                contentDescription = "Previous",
                modifier = Modifier.size(32.dp),
            )
        }

        PlayPauseButton(
            isPlaying = isPlaying,
            isLoading = isLoading,
            onClick = onPlayPauseClicked,
        )

        IconButton(
            onClick = onNextClicked,
            enabled = controlsEnabled,
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
                showIndicator = repeatActive,
            )
        }
    }
}

@Composable
internal fun PlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier.size(64.dp),
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
}

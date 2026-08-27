package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaTextWithIndicator
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.image.MegaIconWithIndicator
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack

@Composable
internal fun PodcastPlaybackControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    controlsEnabled: Boolean,
    currentPlaybackSpeed: Float,
    sleepTimerActive: Boolean,
    sleepTimerContentDescription: String,
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
            enabled = controlsEnabled,
        ) {
            MegaIcon(
                imageVector = IconPack.Medium.Regular.Outline.FifteenBackward,
                tint = IconColor.Primary,
                contentDescription = "Seek backward 15 seconds",
                modifier = Modifier.size(32.dp),
            )
        }

        PlayPauseButton(
            isPlaying = isPlaying,
            isLoading = isLoading,
            onClick = onPlayPauseClicked,
        )

        IconButton(
            onClick = onSeekForward15,
            enabled = controlsEnabled,
        ) {
            MegaIcon(
                imageVector = IconPack.Medium.Regular.Outline.FifteenForward,
                tint = IconColor.Primary,
                contentDescription = "Seek forward 15 seconds",
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(onClick = onSleepTimerClicked) {
            MegaIconWithIndicator(
                imageVector = IconPack.Medium.Thin.Outline.ClockStopwatchShort,
                contentDescription = sleepTimerContentDescription,
                tint = if (sleepTimerActive) IconColor.Brand else IconColor.Primary,
                showIndicator = sleepTimerActive,
            )
        }
    }
}

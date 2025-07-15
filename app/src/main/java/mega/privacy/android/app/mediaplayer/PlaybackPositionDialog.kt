package mega.privacy.android.app.mediaplayer

import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.app.R
import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.presentation.videoplayer.model.PlaybackPositionStatus
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun PlaybackPositionDialog(
    type: MediaType,
    showPlaybackDialog: Boolean,
    currentPlayingItemName: String,
    playbackPosition: Long,
    onPlaybackPositionStatusUpdated: (PlaybackPositionStatus) -> Unit,
) {
    if (showPlaybackDialog) {
        MegaAlertDialog(
            title = stringResource(
                if (type == MediaType.Audio) {
                    sharedR.string.audio_playback_position_dialog_title
                } else {
                    R.string.video_playback_position_dialog_title
                }
            ),
            body = stringResource(
                if (type == MediaType.Audio) {
                    sharedR.string.audio_playback_position_dialog_message
                } else {
                    R.string.video_playback_position_dialog_message
                },
                currentPlayingItemName,
                formatSecondsToString(playbackPosition)
            ),
            confirmButtonText = stringResource(
                if (type == MediaType.Audio) {
                    sharedR.string.audio_playback_position_dialog_restart_button
                } else {
                    R.string.video_playback_position_dialog_restart_button
                }
            ),
            cancelButtonText = stringResource(
                if (type == MediaType.Audio) {
                    sharedR.string.audio_playback_position_dialog_resume_button
                } else {
                    R.string.video_playback_position_dialog_resume_button
                }
            ),
            onConfirm = { onPlaybackPositionStatusUpdated(PlaybackPositionStatus.Restart) },
            onCancel = { onPlaybackPositionStatusUpdated(PlaybackPositionStatus.Resume) },
            onDismiss = {
                onPlaybackPositionStatusUpdated(
                    if (type == MediaType.Audio) {
                        PlaybackPositionStatus.Restart
                    } else {
                        PlaybackPositionStatus.Initial
                    }
                )
            },
        )
    }
}

@SuppressLint("DefaultLocale")
private fun formatSecondsToString(milliseconds: Long): String {
    val duration = milliseconds.milliseconds

    return duration.toComponents { hours, minutes, seconds, _ ->
        if (hours >= 1) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
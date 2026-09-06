package mega.privacy.android.feature.mediaplayer.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.mediaplayer.data.AudioPlayerError
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Terminal playback error dialog for the audio player.
 *
 * Not dismissible by outside taps or back press: the error ends the playback session, so the
 * only way out is acknowledging it (which closes the player screen).
 *
 * @param error The error to display
 * @param onDismiss Invoked when the user acknowledges the error
 * @param modifier Modifier for the dialog root
 */
@Composable
internal fun AudioPlayerErrorDialog(
    error: AudioPlayerError,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Deliberately reuses existing translated strings; the pdf_viewer_ key carries the exact
    // copy the legacy audio player used for its no-network error.
    val message = when (error) {
        is AudioPlayerError.NodeBlocked ->
            stringResource(sharedR.string.file_info_taken_down_file_warning)

        is AudioPlayerError.RetryExhausted ->
            if (error.isNetworkAvailable) {
                stringResource(sharedR.string.unable_to_open_selected_file_message)
            } else {
                stringResource(sharedR.string.pdf_viewer_error_no_network)
            }

        is AudioPlayerError.QueueBuildFailed ->
            if (error.isNetworkAvailable) {
                stringResource(sharedR.string.unable_to_open_selected_file_message)
            } else {
                stringResource(sharedR.string.pdf_viewer_error_no_network)
            }
    }

    BasicDialog(
        modifier = modifier.testTag(AUDIO_PLAYER_ERROR_DIALOG_TAG),
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismiss = onDismiss,
        description = message,
        positiveButtonText = stringResource(sharedR.string.general_ok_only),
        onPositiveButtonClicked = onDismiss,
    )
}

private class AudioPlayerErrorPreviewProvider : PreviewParameterProvider<AudioPlayerError> {
    override val values = sequenceOf(
        AudioPlayerError.RetryExhausted(isNetworkAvailable = true),
        AudioPlayerError.RetryExhausted(isNetworkAvailable = false),
        AudioPlayerError.NodeBlocked,
        AudioPlayerError.QueueBuildFailed(isNetworkAvailable = true),
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewAudioPlayerErrorDialog(
    @PreviewParameter(AudioPlayerErrorPreviewProvider::class) error: AudioPlayerError,
) {
    AndroidThemeForPreviews {
        AudioPlayerErrorDialog(
            error = error,
            onDismiss = {},
        )
    }
}

internal const val AUDIO_PLAYER_ERROR_DIALOG_TAG = "audio_player:error_dialog"

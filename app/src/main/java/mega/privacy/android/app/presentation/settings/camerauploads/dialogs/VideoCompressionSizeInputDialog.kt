package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.legacy.core.ui.controls.dialogs.InputDialog
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] Dialog that allows the User to input a new maximum aggregate Video Size that can
 * be compressed without having to charge the Device
 *
 * @param onNewSizeProvided Lambda to execute when a valid Video Size has been provided
 * @param onDismiss Lambda to execute when the Dialog has been dismissed
 */
@Composable
internal fun VideoCompressionSizeInputDialog(
    onNewSizeProvided: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var showErrorMessage by rememberSaveable { mutableStateOf(false) }

    InputDialog(
        modifier = Modifier.testTag(VIDEO_COMPRESSION_SIZE_INPUT_DIALOG),
        title = stringResource(R.string.settings_video_compression_queue_size_popup_title),
        keyboardActions = KeyboardActions(onDone = {
            handleVideoCompressionSizeInput(
                newVideoCompressionSizeInput = input,
                onErrorMessageVisibilityChanged = { showErrorMessage = it },
                onVideoCompressionSizeValidated = onNewSizeProvided,
            )
        }),
        keyboardType = KeyboardType.Number,
        hint = stringResource(R.string.label_mega_byte),
        confirmButtonText = stringResource(R.string.general_ok),
        cancelButtonText = stringResource(android.R.string.cancel),
        text = input,
        onInputChange = { input = it },
        error = stringResource(
            R.string.settings_compression_queue_subtitle,
            stringResource(R.string.label_file_size_mega_byte, MIN_COMPRESSION_SIZE.toString()),
            stringResource(R.string.label_file_size_mega_byte, MAX_COMPRESSION_SIZE.toString()),
        ).takeIf { showErrorMessage },
        onConfirm = { newVideoCompressionSizeInput ->
            handleVideoCompressionSizeInput(
                newVideoCompressionSizeInput = newVideoCompressionSizeInput,
                onErrorMessageVisibilityChanged = { showErrorMessage = it },
                onVideoCompressionSizeValidated = onNewSizeProvided,
            )
        },
        onDismiss = onDismiss,
    )
}

/**
 * Checks the new Video Compression Size inputted by the User
 *
 * @param newVideoCompressionSizeInput The new Video Compression Size
 * @param onErrorMessageVisibilityChanged Lambda to execute when changing the Dialog Error Message
 * visibility
 * @param onVideoCompressionSizeValidated Lambda to execute when the new Video Compression Size is
 * valid
 */
private fun handleVideoCompressionSizeInput(
    newVideoCompressionSizeInput: String,
    onErrorMessageVisibilityChanged: (Boolean) -> Unit,
    onVideoCompressionSizeValidated: (Int) -> Unit,
) {
    (newVideoCompressionSizeInput.toIntOrNull() ?: 0).let { size ->
        if (size in MIN_COMPRESSION_SIZE..MAX_COMPRESSION_SIZE) {
            onErrorMessageVisibilityChanged.invoke(false)
            onVideoCompressionSizeValidated.invoke(size)
        } else {
            onErrorMessageVisibilityChanged.invoke(true)
        }
    }
}

/**
 * A Preview [Composable] for [VideoCompressionSizeInputDialog]
 */
@CombinedThemePreviews
@Composable
private fun VideoCompressionSizeInputDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoCompressionSizeInputDialog(
            onNewSizeProvided = {},
            onDismiss = {},
        )
    }
}

/**
 * Test Tag for the Video Compression Size Input Dialog
 */
internal const val VIDEO_COMPRESSION_SIZE_INPUT_DIALOG =
    "video_compression_size_input_dialog:input_dialog"

/**
 * Constants that define the minimum and maximum Video Compression Sizes that can be inputted
 */
private const val MIN_COMPRESSION_SIZE = 100
private const val MAX_COMPRESSION_SIZE = 1000
package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] Dialog that displays a list of Video Quality Options for Videos being uploaded by
 * Camera Uploads
 *
 * @param currentVideoQualityUiItem The current [VideoQualityUiItem]
 * @param onOptionSelected Lambda to execute when a new Option has been selected
 * @param onDismissRequest Lambda to execute when the Dialog has been dismissed
 */
@Composable
internal fun VideoQualityDialog(
    currentVideoQualityUiItem: VideoQualityUiItem,
    onOptionSelected: (VideoQualityUiItem) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ConfirmationDialogWithRadioButtons(
        modifier = Modifier.testTag(VIDEO_QUALITY_DIALOG),
        titleText = stringResource(R.string.settings_video_upload_quality),
        initialSelectedOption = currentVideoQualityUiItem,
        radioOptions = VideoQualityUiItem.entries.toList(),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onOptionSelected = onOptionSelected,
        onDismissRequest = onDismissRequest,
        optionDescriptionMapper = { stringResource(it.textRes) },
    )
}

/**
 * A Preview [Composable] for [VideoQualityDialog]
 */
@CombinedThemePreviews
@Composable
private fun VideoQualityDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoQualityDialog(
            currentVideoQualityUiItem = VideoQualityUiItem.Low,
            onOptionSelected = {},
            onDismissRequest = {},
        )
    }
}

/**
 * Test Tag for the Video Quality Dialog
 */
internal const val VIDEO_QUALITY_DIALOG =
    "video_quality_dialog:confirmation_dialog_with_radio_buttons"
package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] Dialog that displays a list of Options on the type of content being uploaded by
 * Camera Uploads
 *
 * @param currentUploadOptionUiItem The current [UploadOptionUiItem]
 * @param onOptionSelected Lambda to execute when a new Option has been selected
 * @param onDismissRequest Lambda to execute when the Dialog has been dismissed
 */
@Composable
internal fun FileUploadDialog(
    currentUploadOptionUiItem: UploadOptionUiItem,
    onOptionSelected: (UploadOptionUiItem) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ConfirmationDialogWithRadioButtons(
        modifier = Modifier.testTag(FILE_UPLOAD_DIALOG),
        titleText = stringResource(R.string.settings_camera_upload_what_to_upload),
        initialSelectedOption = currentUploadOptionUiItem,
        radioOptions = UploadOptionUiItem.entries.toList(),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onOptionSelected = onOptionSelected,
        onDismissRequest = onDismissRequest,
        optionDescriptionMapper = { stringResource(it.textRes) }
    )
}

/**
 * A Preview [Composable] for [FileUploadDialog]
 */
@CombinedThemePreviews
@Composable
private fun FileUploadDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        FileUploadDialog(
            currentUploadOptionUiItem = UploadOptionUiItem.PhotosOnly,
            onOptionSelected = {},
            onDismissRequest = {},
        )
    }
}

/**
 * Test Tag for the File Upload Dialog
 */
internal const val FILE_UPLOAD_DIALOG = "file_upload_dialog:confirmation_dialog_with_radio_buttons"
package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] Dialog that displays a list of Options on how Camera Uploads content should be
 * uploaded
 *
 * @param currentUploadConnectionType The current [UploadConnectionType]
 * @param onOptionSelected Lambda to execute when a new Option has been selected
 * @param onDismissRequest Lambda to execute when the Dialog has been dismissed
 */
@Composable
internal fun HowToUploadDialog(
    currentUploadConnectionType: UploadConnectionType,
    onOptionSelected: (UploadConnectionType) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ConfirmationDialogWithRadioButtons(
        modifier = Modifier.testTag(HOW_TO_UPLOAD_DIALOG),
        titleText = stringResource(R.string.settings_camera_upload_how_to_upload),
        initialSelectedOption = currentUploadConnectionType,
        radioOptions = UploadConnectionType.entries.toList(),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onOptionSelected = onOptionSelected,
        onDismissRequest = onDismissRequest,
        optionDescriptionMapper = { stringResource(it.textRes) },
    )
}

/**
 * A Preview [Composable] for [HowToUploadDialog]
 */
@CombinedThemePreviews
@Composable
private fun HowToUploadDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        HowToUploadDialog(
            currentUploadConnectionType = UploadConnectionType.WIFI,
            onOptionSelected = {},
            onDismissRequest = {},
        )
    }
}

/**
 * Test Tag for the How to Upload Dialog
 */
internal const val HOW_TO_UPLOAD_DIALOG =
    "how_to_upload_dialog:confirmation_dialog_with_radio_buttons"
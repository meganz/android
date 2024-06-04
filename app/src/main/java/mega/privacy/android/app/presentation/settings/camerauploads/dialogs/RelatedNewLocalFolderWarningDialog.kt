package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * A Composable Dialog shown when the newly selected Local Primary / Secondary Dark is related to the
 * opposite Local Folder
 *
 * @param onWarningAcknowledged Lambda to execute upon clicking the Button
 * @param onWarningDismissed Lambda to execute upon clicking outside the Dialog bounds
 */
@Composable
internal fun RelatedNewLocalFolderWarningDialog(
    onWarningAcknowledged: () -> Unit,
    onWarningDismissed: () -> Unit,
) {
    MegaAlertDialog(
        modifier = Modifier.testTag(RELATED_NEW_LOCAL_FOLDER_WARNING_DIALOG),
        title = stringResource(SharedR.string.settings_camera_uploads_related_new_local_folder_warning_dialog_title),
        body = stringResource(SharedR.string.settings_camera_uploads_related_new_local_folder_warning_dialog_body),
        confirmButtonText = stringResource(SharedR.string.settings_camera_uploads_related_new_local_folder_warning_dialog_confirm_button),
        cancelButtonText = null,
        onConfirm = onWarningAcknowledged,
        onDismiss = onWarningDismissed,
    )
}

/**
 * A Preview [Composable] for [RelatedNewLocalFolderWarningDialog]
 */
@CombinedThemePreviews
@Composable
private fun RelatedNewLocalFolderWarningDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        RelatedNewLocalFolderWarningDialog(
            onWarningAcknowledged = {},
            onWarningDismissed = {},
        )
    }
}

/**
 * Test Tag for the Non Unique Local Folder Warning Dialog
 */
internal const val RELATED_NEW_LOCAL_FOLDER_WARNING_DIALOG =
    "related_new_local_folder_warning_dialog:mega_alert_dialog"
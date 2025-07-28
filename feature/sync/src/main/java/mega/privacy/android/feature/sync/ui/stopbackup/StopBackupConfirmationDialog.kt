package mega.privacy.android.feature.sync.ui.stopbackup

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.ui.model.StopBackupOption
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun StopBackupConfirmationDialog(
    onConfirm: (option: StopBackupOption, selectedFolder: RemoteFolder?) -> Unit,
    onDismiss: () -> Unit,
    onSelectStopBackupDestinationClicked: (String?) -> Unit,
    folderName: String?,
    modifier: Modifier = Modifier,
) {

    StopBackupConfirmationDialogBody(
        onConfirm = onConfirm,
        onDismiss = {
            onDismiss()
        },
        folderName = folderName,
        onSelectStopBackupDestinationClicked = onSelectStopBackupDestinationClicked,
        modifier = modifier,
    )
}

@Composable
internal fun StopBackupConfirmationDialogBody(
    onConfirm: (option: StopBackupOption, selectedFolder: RemoteFolder?) -> Unit,
    onDismiss: () -> Unit,
    folderName: String?,
    onSelectStopBackupDestinationClicked: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConfirmationDialog(
        title = stringResource(id = sharedR.string.sync_stop_backup_confirm_dialog_title),
        text1 = stringResource(sharedR.string.sync_stop_backup_confirm_dialog_text),
        text2 = stringResource(sharedR.string.sync_stop_backup_confirm_dialog_delete_explanation),
        buttonOption1Text = stringResource(sharedR.string.sync_stop_backup_confirm_dialog_move_cloud_drive),
        buttonOption2Text = stringResource(sharedR.string.sync_stop_backup_confirm_dialog_delete_permanently),
        cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onOption1 = {
            onSelectStopBackupDestinationClicked(folderName)
        },
        onOption2 = {
            onConfirm(StopBackupOption.DELETE, null)
        },
        onDismiss = onDismiss,
        modifier = modifier.testTag(
            STOP_BACKUP_CONFIRMATION_DIALOG_BODY_TEST_TAG
        )
    )

}

@CombinedThemePreviews
@Composable
private fun StopBackupConfirmationDialogBodyPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaScaffold {
            StopBackupConfirmationDialogBody(
                onConfirm = { _, _ -> },
                onDismiss = {},
                folderName = null,
                onSelectStopBackupDestinationClicked = {},
            )
        }
    }
}

internal const val STOP_BACKUP_CONFIRMATION_DIALOG_BODY_TEST_TAG =
    "stop_backup_confirmation_dialog:body"
internal const val STOP_BACKUP_CONFIRMATION_DIALOG_MOVE_OPTION_ROW_TEST_TAG =
    "stop_backup_confirmation_dialog:move_option_row"
internal const val STOP_BACKUP_CONFIRMATION_DIALOG_DELETE_OPTION_ROW_TEST_TAG =
    "stop_backup_confirmation_dialog:delete_option_row"
internal const val STOP_BACKUP_CONFIRMATION_DIALOG_MOVE_OPTION_SELECT_DESTINATION_TEST_TAG =
    "stop_backup_confirmation_dialog:move_option_select_destination"

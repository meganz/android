package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoExtraAction
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationWithRadioButtonsDialog
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.domain.entity.shares.AccessPermission

@Composable
internal fun ExtraActionDialog(
    action: FileInfoExtraAction,
    onRemoveConfirmed: () -> Unit,
    onPermissionSelected: (AccessPermission, emails: List<String>) -> Unit,
    onDismiss: () -> Unit,
) = when (action) {
    is FileInfoExtraAction.ConfirmRemove -> {
        MegaAlertDialog(
            text = action.text,
            confirmButtonText = stringResource(id = R.string.general_remove),
            cancelButtonText = stringResource(id = R.string.general_cancel),
            onConfirm = onRemoveConfirmed,
            onDismiss = onDismiss,
        )
    }

    is FileInfoExtraAction.ChangePermission -> {
        ConfirmationWithRadioButtonsDialog(
            radioOptions = listOf(
                AccessPermission.READ,
                AccessPermission.READWRITE,
                AccessPermission.FULL
            ),
            initialSelectedOption = action.selected,
            titleText = stringResource(id = R.string.file_properties_shared_folder_permissions),
            buttonText = stringResource(id = R.string.general_cancel),
            onOptionSelected = {
                onPermissionSelected(it, action.emails)
            },
            onDismissRequest = onDismiss,
            optionDescriptionMapper = { permission ->
                when (permission) {
                    AccessPermission.READ -> stringResource(id = R.string.file_properties_shared_folder_read_only)
                    AccessPermission.READWRITE -> stringResource(id = R.string.file_properties_shared_folder_read_write)
                    else -> stringResource(id = R.string.file_properties_shared_folder_full_access)
                }
            }
        )
    }
}
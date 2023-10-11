package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoExtraAction
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
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
        ConfirmationDialogWithRadioButtons(
            radioOptions = listOf(
                AccessPermission.READ,
                AccessPermission.READWRITE,
                AccessPermission.FULL
            ),
            initialSelectedOption = action.selected,
            titleText = stringResource(id = R.string.file_properties_shared_folder_permissions),
            cancelButtonText = stringResource(id = R.string.general_cancel),
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

@CombinedThemeRtlPreviews
@Composable
private fun ExtraActionDialogPreview(
    @PreviewParameter(ExtraActionDialogPreviewProvider::class) action: FileInfoExtraAction,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ExtraActionDialog(
            action = action,
            onRemoveConfirmed = { },
            onPermissionSelected = { _, _ -> },
            onDismiss = {}
        )
    }
}

private class ExtraActionDialogPreviewProvider : PreviewParameterProvider<FileInfoExtraAction> {
    override val values: Sequence<FileInfoExtraAction>
        get() = sequenceOf(
            FileInfoExtraAction.ConfirmRemove.SendToRubbish,
            FileInfoExtraAction.ChangePermission(
                listOf("email1@example.com", "email2@example.com"),
                AccessPermission.READ
            )
        )

}
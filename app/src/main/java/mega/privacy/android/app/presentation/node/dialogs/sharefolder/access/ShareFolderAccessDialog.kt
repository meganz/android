package mega.privacy.android.app.presentation.node.dialogs.sharefolder.access

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderAccessDialogViewModel
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * Share folder access dialog to show access dialog to share folder
 * @param handles List of handle
 * @param contactData list of contact with whom folders needed to be shared
 * @param viewModel [ShareFolderAccessDialog]
 * @param onDismiss
 */
@Composable
fun ShareFolderAccessDialog(
    handles: List<Long>,
    contactData: List<String>,
    isFromBackups: Boolean,
    viewModel: ShareFolderAccessDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {

    if (isFromBackups) {
        viewModel.shareFolder(
            handles = handles,
            contactData = contactData,
            accessPermission = AccessPermission.READ
        )
        onDismiss()
    }
    ShareFolderAccessDialogBody(
        radioButtonOptions = listOf(
            AccessPermission.READ,
            AccessPermission.READWRITE,
            AccessPermission.FULL
        ),
        onItemSelected = {
            viewModel.shareFolder(
                handles = handles,
                contactData = contactData,
                accessPermission = it
            )
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun ShareFolderAccessDialogBody(
    radioButtonOptions: List<AccessPermission>,
    onItemSelected: (AccessPermission) -> Unit,
    onDismiss: () -> Unit,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ConfirmationDialogWithRadioButtons(
            radioOptions = radioButtonOptions,
            titleText = stringResource(id = sharedResR.string.dialog_select_permissions),
            onOptionSelected = {
                onItemSelected(it)
            },
            optionDescriptionMapper = {
                when (it) {
                    AccessPermission.READ -> stringResource(id = sharedResR.string.file_properties_shared_folder_read_only)
                    AccessPermission.READWRITE -> stringResource(id = sharedResR.string.file_properties_shared_folder_read_write)
                    else -> stringResource(id = sharedResR.string.file_properties_shared_folder_full_access)
                }
            },
            onDismissRequest = onDismiss,
            cancelButtonText = null,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ShareFolderAccessDialogBodyPreview() {
    ShareFolderAccessDialogBody(
        radioButtonOptions = listOf(
            AccessPermission.READ,
            AccessPermission.READWRITE,
            AccessPermission.FULL
        ), onItemSelected = {},
        onDismiss = {}
    )
}
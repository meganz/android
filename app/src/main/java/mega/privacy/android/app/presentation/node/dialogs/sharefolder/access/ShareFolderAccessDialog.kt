package mega.privacy.android.app.presentation.node.dialogs.sharefolder.access

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.shared.theme.MegaAppTheme

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
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ConfirmationDialogWithRadioButtons(
            radioOptions = radioButtonOptions,
            titleText = stringResource(id = R.string.dialog_select_permissions),
            onOptionSelected = {
                onItemSelected(it)
            },
            optionDescriptionMapper = {
                when (it) {
                    AccessPermission.READ -> stringResource(id = R.string.file_properties_shared_folder_read_only)
                    AccessPermission.READWRITE -> stringResource(id = R.string.file_properties_shared_folder_read_write)
                    else -> stringResource(id = R.string.file_properties_shared_folder_full_access)
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
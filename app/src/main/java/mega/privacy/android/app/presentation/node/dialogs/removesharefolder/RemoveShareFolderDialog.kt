package mega.privacy.android.app.presentation.node.dialogs.removesharefolder

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Dialog to remove folder share
 * @param nodeList List of node to be removed from folder share
 * @param onDismiss
 * @param viewModel [RemoveShareFolderViewModel]
 */
@Composable
fun RemoveShareFolderDialog(
    nodeList: List<NodeId>,
    onDismiss: () -> Unit,
    viewModel: RemoveShareFolderViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getContactInfoForSharedFolder(nodeList)
    }
    RemoveShareFolderDialogBody(
        state = state, onConfirm = {
            viewModel.removeShare(nodeList)
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun RemoveShareFolderDialogBody(
    state: RemoveShareFolderState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        val text = if (state.numberOfShareFolder == 1) {
            pluralStringResource(
                R.plurals.confirmation_remove_outgoing_shares,
                state.numberOfShareContact,
                state.numberOfShareContact
            )
        } else {
            pluralStringResource(
                R.plurals.alert_remove_several_shares,
                state.numberOfShareFolder,
                state.numberOfShareFolder
            )
        }
        val confirmButtonText = if (state.numberOfShareFolder == 1) {
            stringResource(id = R.string.general_remove)
        } else {
            stringResource(id = R.string.shared_items_outgoing_unshare_confirm_dialog_button_yes)
        }
        val cancelButtonText = if (state.numberOfShareFolder == 1) {
            stringResource(id = R.string.general_cancel)
        } else {
            stringResource(id = R.string.shared_items_outgoing_unshare_confirm_dialog_button_no)
        }
        MegaAlertDialog(
            text = text,
            confirmButtonText = confirmButtonText,
            cancelButtonText = cancelButtonText,
            onConfirm = {
                onConfirm()
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RemoveShareFolderBodyPreview() {
    RemoveShareFolderDialogBody(
        state = RemoveShareFolderState(
            numberOfShareFolder = 1,
            numberOfShareContact = 3
        ),
        onConfirm = {},
        onDismiss = {}
    )
}
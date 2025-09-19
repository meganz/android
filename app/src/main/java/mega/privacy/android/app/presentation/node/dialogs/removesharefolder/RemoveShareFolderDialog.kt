package mega.privacy.android.app.presentation.node.dialogs.removesharefolder

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.core.nodecomponents.dialog.removeshare.RemoveShareFolderState
import mega.privacy.android.core.nodecomponents.dialog.removeshare.RemoveShareFolderViewModel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Dialog to remove folder share
 * @param nodeList List of node to be removed from folder share
 * @param onDismiss
 * @param viewModel [mega.privacy.android.core.nodecomponents.dialog.removeshare.RemoveShareFolderViewModel]
 */
@Composable
fun RemoveShareFolderDialog(
    nodeList: List<NodeId>,
    onDismiss: () -> Unit,
    viewModel: RemoveShareFolderViewModel = hiltViewModel(),
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        val text = if (state.numberOfShareFolder == 1) {
            stringResource(sharedR.string.stop_sharing_dialog_title)
        } else {
            stringResource(sharedR.string.stop_sharing_dialog_title_plurals)
        }
        val confirmButtonText =
            stringResource(id = sharedR.string.stop_sharing_dialog_positive_button_text)
        val cancelButtonText =
            stringResource(id = sharedR.string.general_dialog_cancel_button)
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
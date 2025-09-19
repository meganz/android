package mega.privacy.android.core.nodecomponents.dialog.removeshare

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * Dialog to remove folder share
 * @param nodeList List of node to be removed from folder share
 * @param onDismiss
 * @param viewModel [RemoveShareFolderViewModel]
 */
@Composable
fun RemoveShareFolderDialogM3(
    nodes: List<NodeId>,
    onDismiss: () -> Unit,
    viewModel: RemoveShareFolderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getContactInfoForSharedFolder(nodes)
    }

    RemoveShareFolderDialogBodyM3(
        state = state,
        onConfirm = {
            viewModel.removeShare(nodes)
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun RemoveShareFolderDialogBodyM3(
    state: RemoveShareFolderState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val text = if (state.numberOfShareFolder == 1) {
        stringResource(sharedResR.string.stop_sharing_dialog_title)
    } else {
        stringResource(sharedResR.string.stop_sharing_dialog_title_plurals)
    }
    val confirmButtonText =
        stringResource(id = sharedResR.string.stop_sharing_dialog_positive_button_text)
    val cancelButtonText =
        stringResource(id = sharedResR.string.general_dialog_cancel_button)

    BasicDialog(
        description = text,
        positiveButtonText = confirmButtonText,
        negativeButtonText = cancelButtonText,
        onPositiveButtonClicked = {
            onConfirm()
            onDismiss()
        },
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss
    )
}

@CombinedThemePreviews
@Composable
private fun RemoveShareFolderBodyPreview() {
    AndroidThemeForPreviews {
        RemoveShareFolderDialogBodyM3(
            state = RemoveShareFolderState(
                numberOfShareFolder = 1,
                numberOfShareContact = 3
            ),
            onConfirm = {},
            onDismiss = {}
        )
    }
}
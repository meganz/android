package mega.privacy.android.core.nodecomponents.dialog.delete

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.nodes.R as NodesR

/**
 * Dialog to move nodes to rubbish bin or delete them permanently
 *
 * @param nodes List of node handles to process
 * @param onDismiss Callback when dialog is dismissed
 * @param viewModel ViewModel to handle the business logic
 * @param isNodeInRubbish Whether the nodes are already in rubbish bin
 */
@Composable
fun MoveToRubbishOrDeleteNodeDialogM3(
    nodes: List<Long>,
    onDismiss: () -> Unit,
    viewModel: MoveToRubbishOrDeleteNodeDialogViewModel = hiltViewModel(),
    isNodeInRubbish: Boolean = false,
) {
    val (message, positiveText) = remember(isNodeInRubbish) {
        if (isNodeInRubbish) {
            NodesR.string.confirmation_delete_from_mega to NodesR.string.rubbish_bin_delete_confirmation_dialog_button_delete
        } else {
            NodesR.string.confirmation_move_to_rubbish to sharedR.string.general_move
        }
    }

    BasicDialog(
        modifier = Modifier.testTag(MOVE_TO_RUBBISH_OR_DELETE_NODE_DIALOG_TAG),
        description = stringResource(id = message),
        positiveButtonText = stringResource(id = positiveText),
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onPositiveButtonClicked = {
            if (isNodeInRubbish) {
                viewModel.deleteNodes(nodes)
            } else {
                viewModel.moveNodesToRubbishBin(nodes)
            }
            onDismiss()
        },
        onNegativeButtonClicked = onDismiss
    )
}

internal const val MOVE_TO_RUBBISH_OR_DELETE_NODE_DIALOG_TAG =
    "move_to_rubbish_or_delete_node:dialog"

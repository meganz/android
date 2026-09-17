package mega.privacy.android.core.nodecomponents.components.selectionmode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.selection.NodeSelection

/**
 * Keeps a [NodeSelection] and the selection-mode actions in [viewModel] in step: the available
 * actions follow the selected nodes, and the selection is cleared once an action has been
 * triggered or the options were dismissed.
 *
 * @param selection the screen's selection
 * @param nodeSourceType the source the selected nodes belong to
 * @param viewModel the ViewModel driving the selection-mode bottom bar
 */
@Composable
fun NodeSelectionActionsEffect(
    selection: NodeSelection<out TypedNode>,
    nodeSourceType: NodeSourceType,
    viewModel: NodeOptionsActionViewModel,
) {
    val actionState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedNodes = selection.selectedNodes

    LaunchedEffect(selectedNodes, nodeSourceType) {
        viewModel.updateSelectionModeAvailableActions(selectedNodes.toSet(), nodeSourceType)
    }

    EventEffect(
        event = actionState.actionTriggeredEvent,
        onConsumed = viewModel::resetActionTriggered,
    ) {
        selection.deselectAll()
    }

    EventEffect(
        event = actionState.dismissEvent,
        onConsumed = viewModel::resetDismiss,
    ) {
        selection.deselectAll()
    }
}

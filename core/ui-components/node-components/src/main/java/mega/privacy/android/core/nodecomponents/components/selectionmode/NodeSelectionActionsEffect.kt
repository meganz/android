package mega.privacy.android.core.nodecomponents.components.selectionmode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import de.palm.composestateevents.EventEffect
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionIntent
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.selection.NodeSelection

/**
 * Keeps a [NodeSelection] and the selection-mode actions in step: the available actions follow
 * the selected nodes, and the selection is cleared once an action has been triggered or the
 * options were dismissed.
 *
 * @param selection the screen's selection
 * @param nodeSourceType the source the selected nodes belong to
 * @param uiState the state of the selection-mode actions
 * @param onIntent receives what the actions owner should do in response
 */
@Composable
fun NodeSelectionActionsEffect(
    selection: NodeSelection<out TypedNode>,
    nodeSourceType: NodeSourceType,
    uiState: NodeActionState,
    onIntent: (NodeOptionsActionIntent) -> Unit,
) {
    val selectedNodeIds = selection.state.selectedNodeIds

    LaunchedEffect(selectedNodeIds, nodeSourceType) {
        onIntent(
            NodeOptionsActionIntent.SelectedNodesChanged(
                nodes = selection.selectedNodes.toSet(),
                nodeSourceType = nodeSourceType,
            )
        )
    }

    EventEffect(
        event = uiState.actionTriggeredEvent,
        onConsumed = { onIntent(NodeOptionsActionIntent.ActionTriggeredConsumed) },
    ) {
        selection.deselectAll()
    }

    EventEffect(
        event = uiState.dismissEvent,
        onConsumed = { onIntent(NodeOptionsActionIntent.DismissConsumed) },
    ) {
        selection.deselectAll()
    }
}

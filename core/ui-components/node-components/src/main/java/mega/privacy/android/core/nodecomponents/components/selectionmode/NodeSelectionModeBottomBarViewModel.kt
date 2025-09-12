package mega.privacy.android.core.nodecomponents.components.selectionmode

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.mapper.NodeSelectionActionToMenuActionMapper
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

@HiltViewModel
class NodeSelectionModeBottomBarViewModel @Inject constructor(
    private val nodeSelectionActionToMenuActionMapper: NodeSelectionActionToMenuActionMapper,
) : ViewModel() {

    fun handleAction(
        action: NodeSelectionAction,
        selectedNodes: List<TypedNode>,
        nodeActionHandler: NodeActionHandler,
        onMoreActionPressed: () -> Unit = {},
    ) {
        nodeSelectionActionToMenuActionMapper(
            action = action,
            selectedNodes = selectedNodes,
            nodeActionHandler = nodeActionHandler,
            onMoreActionPressed = onMoreActionPressed
        )
    }
}
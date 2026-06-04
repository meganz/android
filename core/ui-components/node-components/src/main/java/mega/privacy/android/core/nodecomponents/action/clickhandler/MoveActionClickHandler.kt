package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.NodeActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.destination.MoveNavKey
import javax.inject.Inject

class MoveActionClickHandler @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is MoveMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        launchMove(listOf(node.id), node.parentId, provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        launchMove(nodes.map { it.id }, nodes.firstOrNull()?.parentId ?: NodeId(-1L), provider)
    }

    /**
     * Move counterpart of [CopyActionClickHandler.launchCopy]. Sources and their current
     * [parentId] travel inside [MoveNavKey] (the parent is used to block re-selecting the
     * current folder as the move target) and come back inside
     * [mega.privacy.android.navigation.destination.MoveResult].
     */
    private fun launchMove(nodeIds: List<NodeId>, parentId: NodeId, provider: NodeActionProvider) {
        val navigationHandler = provider.navigationHandler
        if (navigationHandler != null && provider.viewModel.uiState.value.isCloudExplorerAvailable) {
            navigationHandler.navigate(MoveNavKey(nodeIds = nodeIds, disabledTargetId = parentId))
        } else {
            provider.moveLauncher.launch(nodeIds.map { it.longValue }.toLongArray())
        }
    }
}

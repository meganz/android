package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.NodeActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.destination.CopyNavKey
import javax.inject.Inject

class CopyActionClickHandler @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is CopyMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        launchCopy(listOf(node.id), provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        launchCopy(nodes.map { it.id }, provider)
    }

    /**
     * In the single-activity world navigate to the Compose copy picker; otherwise fall back to the
     * legacy folder-picker activity launcher. The source nodes travel inside [CopyNavKey] and
     * are returned to the host via [mega.privacy.android.navigation.destination.CopyResult].
     */
    private fun launchCopy(nodeIds: List<NodeId>, provider: NodeActionProvider) {
        val navigationHandler = provider.navigationHandler
        if (navigationHandler != null && provider.viewModel.uiState.value.isCloudExplorerAvailable) {
            navigationHandler.navigate(CopyNavKey(nodeIds = nodeIds))
        } else {
            provider.copyLauncher.launch(nodeIds.map { it.longValue }.toLongArray())
        }
    }
}

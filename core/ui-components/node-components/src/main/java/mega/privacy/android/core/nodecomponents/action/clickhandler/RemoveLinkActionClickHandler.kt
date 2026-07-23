package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.NodeActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveLinkMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.dialog.removelink.RemoveNodeLinkDialogNavKey
import javax.inject.Inject

class RemoveLinkActionClickHandler @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is RemoveLinkMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        removeLinks(listOf(node.id.longValue), provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        removeLinks(nodes.map { it.id.longValue }, provider)
    }

    private fun removeLinks(nodes: List<Long>, provider: NodeActionProvider) {
        provider.viewModel.navigateWithNavKey(
            RemoveNodeLinkDialogNavKey(handles = nodes)
        )
    }
}

package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.dialog.leaveshare.LeaveShareDialogNavKey
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.LeaveShareMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class LeaveShareActionClickHandler @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is LeaveShareMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        navigateToLeaveShareDialog(
            nodeHandles = listOf(node.id.longValue),
            navigationHandler = provider.viewModel::navigateWithNavKey
        )
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        navigateToLeaveShareDialog(
            nodeHandles = nodes.map { it.id.longValue },
            navigationHandler = provider.viewModel::navigateWithNavKey
        )
    }

    private fun navigateToLeaveShareDialog(
        nodeHandles: List<Long>,
        navigationHandler: (LeaveShareDialogNavKey) -> Unit,
    ) {
        runCatching {
            nodeHandlesToJsonMapper(nodeHandles)
        }.onSuccess {
            navigationHandler(LeaveShareDialogNavKey(handles = it))
        }
    }
}

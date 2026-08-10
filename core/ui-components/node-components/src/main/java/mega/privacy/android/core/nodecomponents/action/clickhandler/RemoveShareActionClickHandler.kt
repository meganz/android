package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.NodeActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.dialog.removeshare.RemoveShareFolderDialogNavKey
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveShareMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import timber.log.Timber
import javax.inject.Inject

class RemoveShareActionClickHandler @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is RemoveShareMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        removeShares(listOf(node.id.longValue), provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val nodeList = nodes.map { it.id.longValue }
        removeShares(nodeList, provider)
    }

    private fun removeShares(nodeList: List<Long>, provider: NodeActionProvider) {
        runCatching { nodeHandlesToJsonMapper(nodeList) }
            .onSuccess { handles ->
                provider.viewModel.navigateWithNavKey(
                    RemoveShareFolderDialogNavKey(nodes = handles)
                )
            }.onFailure {
                Timber.e(it)
            }
    }
}

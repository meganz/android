package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.dialog.removeshare.RemoveShareFolderDialogArgs
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveShareMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import timber.log.Timber
import javax.inject.Inject

class RemoveShareActionClickHandler @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is RemoveShareMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        val nodeList = listOf(node.id.longValue)
        runCatching { nodeHandlesToJsonMapper(nodeList) }
            .onSuccess { handles ->
                provider.navigationHandler?.navigate(
                    RemoveShareFolderDialogArgs(nodes = handles)
                )
            }.onFailure {
                Timber.e(it)
            }
    }
}

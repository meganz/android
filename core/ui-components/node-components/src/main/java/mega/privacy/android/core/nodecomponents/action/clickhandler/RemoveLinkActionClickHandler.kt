package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.dialog.removelink.RemoveNodeLinkDialogNavKey
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveLinkMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class RemoveLinkActionClickHandler @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is RemoveLinkMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.navigationHandler?.navigate(
            RemoveNodeLinkDialogNavKey(
                nodes = nodeHandlesToJsonMapper(listOf(node.id.longValue))
            )
        )
    }
}

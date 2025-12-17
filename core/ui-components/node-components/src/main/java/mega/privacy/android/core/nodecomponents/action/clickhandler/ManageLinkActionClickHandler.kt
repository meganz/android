package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class ManageLinkActionClickHandler @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is ManageLinkMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.megaNavigator.openGetLinkActivity(
            context = provider.context,
            node.id.longValue
        )
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val handles = nodes.map { it.id.longValue }.toLongArray()
        provider.megaNavigator.openGetLinkActivity(
            context = provider.context,
            *handles
        )
    }
}

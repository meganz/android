package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.RenameMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class RenameNodeActionClickHandler @Inject constructor() : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean =
        action is RenameMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.viewModel.handleRenameNodeRequest(node.id)
    }
}

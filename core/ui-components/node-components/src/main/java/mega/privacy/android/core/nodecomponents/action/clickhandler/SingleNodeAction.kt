package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Handler for single node operations.
 */
interface SingleNodeAction : BaseNodeAction {
    fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider)
}

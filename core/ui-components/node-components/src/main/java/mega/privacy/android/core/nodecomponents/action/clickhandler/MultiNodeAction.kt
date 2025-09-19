package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Handler for multiple nodes operations.
 */
interface MultiNodeAction : BaseNodeAction {
    fun handle(action: MenuAction, nodes: List<TypedNode>, provider: MultipleNodesActionProvider)
}

package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.dialog.leaveshare.LeaveShareDialogArgs
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.LeaveShareMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class LeaveShareActionClickHandler @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is LeaveShareMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        runCatching {
            nodeHandlesToJsonMapper(listOf(node.id.longValue))
        }.onSuccess {
            provider.navigationHandler?.navigate(
                LeaveShareDialogArgs(handles = it)
            )
        }
    }
}

package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.SaveToMegaMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class SaveToMegaActionClickHandler @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is SaveToMegaMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        if (provider.viewModel.uiState.value.isLoggedIn) {
            provider.copyLauncher.launch(longArrayOf(node.id.longValue))
        } else {
            provider.viewModel.postMessage("You need to login first") // TODO to be confirmed
            // TODO navigate to login screen
        }
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        if (provider.viewModel.uiState.value.isLoggedIn) {
            val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
            provider.copyLauncher.launch(nodeHandleArray)
        } else {
            provider.viewModel.postMessage("You need to login first") // TODO to be confirmed
            // TODO navigate to login screen
        }
    }
}

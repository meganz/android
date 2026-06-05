package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.SaveToMegaMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.navigation.destination.ImportNavKey
import javax.inject.Inject

class SaveToMegaActionClickHandler @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is SaveToMegaMenuAction

    override fun handle(
        action: MenuAction,
        node: TypedNode,
        provider: SingleNodeActionProvider,
    ) {
        if (!provider.viewModel.uiState.value.isLoggedIn) {
            provider.viewModel.triggerLoginRequiredEvent()
            return
        }

        val navigationHandler = provider.navigationHandler
        if (navigationHandler != null && provider.viewModel.uiState.value.isCloudExplorerAvailable) {
            navigationHandler.navigate(ImportNavKey)
        } else if (node is PublicLinkFile) {
            provider.publicCopyLauncher.launch(longArrayOf(node.id.longValue))
        } else {
            provider.copyLauncher.launch(longArrayOf(node.id.longValue))
        }
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        if (!provider.viewModel.uiState.value.isLoggedIn) {
            provider.viewModel.triggerLoginRequiredEvent()
            return
        }
        val navigationHandler = provider.navigationHandler
        if (navigationHandler != null && provider.viewModel.uiState.value.isCloudExplorerAvailable) {
            navigationHandler.navigate(ImportNavKey)
        } else {
            provider.copyLauncher.launch(nodes.map { it.id.longValue }.toLongArray())
        }
    }
}

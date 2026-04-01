package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.InfoMenuAction
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.destination.OfflineInfoNavKey
import javax.inject.Inject

class InfoActionClickHandler @Inject constructor(
    private val megaNavigator: MegaNavigator,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is InfoMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        val nodeSourceType = provider.viewModel.getNodeSourceType()
        if (nodeSourceType == NodeSourceType.OFFLINE) {
            // For offline files, navigate to OfflineInfoNavKey
            provider.navigationHandler?.navigate(OfflineInfoNavKey(handle = node.id.longValue.toString()))
        } else {
            megaNavigator.openFileInfoActivity(
                context = provider.context,
                handle = node.id.longValue
            )
        }

        provider.viewModel.dismiss()
    }
}

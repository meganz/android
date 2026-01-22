package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject
import mega.privacy.mobile.analytics.event.LinkManageLinkTapFileMenuItemEvent
import mega.privacy.mobile.analytics.event.LinkManageLinkTapFolderMenuItemEvent
import mega.privacy.mobile.analytics.event.LinkManageLinkTapFileMenuToolbarEvent
import mega.privacy.mobile.analytics.event.LinkManageLinkTapFolderMenuToolbarEvent
import mega.privacy.mobile.analytics.event.LinkGetLinkForNodesMenuToolbarEvent

class ManageLinkActionClickHandler @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is ManageLinkMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        Analytics.tracker.trackEvent(if (node is FolderNode) LinkManageLinkTapFolderMenuItemEvent else LinkManageLinkTapFileMenuItemEvent)

        provider.megaNavigator.openGetLinkActivity(
            context = provider.context,
            node.id.longValue
        )
        provider.viewModel.dismiss()
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        if (nodes.size == 1) {
            Analytics.tracker.trackEvent(if (nodes.first() is FolderNode) LinkManageLinkTapFolderMenuToolbarEvent else LinkManageLinkTapFileMenuToolbarEvent)
        } else {
            Analytics.tracker.trackEvent(LinkGetLinkForNodesMenuToolbarEvent)
        }

        val handles = nodes.map { it.id.longValue }.toLongArray()
        provider.megaNavigator.openGetLinkActivity(
            context = provider.context,
            *handles
        )
        provider.viewModel.dismiss()
    }
}

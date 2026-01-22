package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.GetLinkMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.mobile.analytics.event.LinkShareLinkTapFileMenuItemEvent
import mega.privacy.mobile.analytics.event.LinkShareLinkTapFolderMenuItemEvent
import mega.privacy.mobile.analytics.event.LinkShareLinkTapFileMenuToolbarEvent
import mega.privacy.mobile.analytics.event.LinkShareLinkTapFolderMenuToolbarEvent
import mega.privacy.mobile.analytics.event.LinkShareLinkForNodesMenuToolbarEvent

class GetLinkActionClickHandler @Inject constructor(
    private val megaNavigator: MegaNavigator,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is GetLinkMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        Analytics.tracker.trackEvent(if (node is FolderNode) LinkShareLinkTapFolderMenuItemEvent else LinkShareLinkTapFileMenuItemEvent)

        megaNavigator.openGetLinkActivity(
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
            Analytics.tracker.trackEvent(if (nodes.first() is FolderNode) LinkShareLinkTapFolderMenuToolbarEvent else LinkShareLinkTapFileMenuToolbarEvent)
        } else {
            Analytics.tracker.trackEvent(LinkShareLinkForNodesMenuToolbarEvent)
        }
        megaNavigator.openGetLinkActivity(
            context = provider.context,
            *nodes.map { it.id.longValue }.toLongArray()
        )
        provider.viewModel.dismiss()
    }
}

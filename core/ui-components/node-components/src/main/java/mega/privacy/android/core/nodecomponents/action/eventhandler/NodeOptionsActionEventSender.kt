package mega.privacy.android.core.nodecomponents.action.eventhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.node.NodeSourceType
import javax.inject.Inject

class NodeOptionsActionEventSender @Inject constructor(
    private val nodeOptionsActionEventMapper: NodeOptionsActionEventMapper,
) {
    operator fun invoke(menuAction: MenuAction, nodeSourceType: NodeSourceType?) {
        val event = nodeOptionsActionEventMapper(menuAction, nodeSourceType) ?: return
        Analytics.tracker.trackEvent(event)
    }
}
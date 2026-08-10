package mega.privacy.android.core.nodecomponents.action.eventhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.eventhandler.mapper.CloudDriveActionEventMapper
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import javax.inject.Inject

class NodeOptionsActionEventMapper @Inject constructor(
    private val cloudDriveActionEventMapper: CloudDriveActionEventMapper,
) {
    operator fun invoke(menuAction: MenuAction, nodeSourceType: NodeSourceType?): EventIdentifier? {
        return when (nodeSourceType) {
            NodeSourceType.CLOUD_DRIVE -> cloudDriveActionEventMapper(menuAction)
            else -> null
        }
    }
}
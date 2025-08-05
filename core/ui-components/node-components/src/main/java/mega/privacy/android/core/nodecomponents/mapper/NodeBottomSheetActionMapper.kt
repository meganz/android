package mega.privacy.android.core.nodecomponents.mapper

import mega.privacy.android.core.nodecomponents.model.NodeActionModeMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Node bottom sheet action mapper
 *
 * Helps to get bottom sheet actions based on the nodes selected
 */
class NodeBottomSheetActionMapper @Inject constructor() {

    /**
     * Invocation
     *
     * @param toolbarOptions all the toolbar options available for selected screen
     * @param selectedNode selected node
     */
    suspend operator fun invoke(
        toolbarOptions: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<*>>,
        selectedNode: TypedNode,
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackUps: Boolean,
        isConnected: Boolean,
    ) = toolbarOptions.filter {
        it.shouldDisplay(
            isNodeInRubbish = isNodeInRubbish,
            accessPermission = accessPermission,
            isInBackups = isInBackUps,
            node = selectedNode,
            isConnected = isConnected,
        )
    }.map {
        NodeActionModeMenuItem(
            group = it.groupId,
            orderInGroup = it.menuAction.orderInCategory,
            control = it.buildComposeControl(selectedNode)
        )
    }

}
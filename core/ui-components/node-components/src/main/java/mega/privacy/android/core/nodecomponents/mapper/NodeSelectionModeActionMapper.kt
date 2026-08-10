package mega.privacy.android.core.nodecomponents.mapper

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionModeMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Node selection mode action mapper
 */
class NodeSelectionModeActionMapper @Inject constructor() {

    /**
     * Invocation
     *
     * @param options all the options available for selection mode
     * @param selectedNodes selected nodes
     * @param hasNodeAccessPermission checks if node has rename permission
     * @param noNodeInBackups checks if no node is part of back ups
     * @param allNodeCanBeMovedToTarget checks if node can be moved to rubbish bin
     */
    suspend operator fun invoke(
        options: Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>>,
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        allNodeCanBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        nodeSourceType: NodeSourceType,
    ): List<NodeSelectionModeMenuItem> {
        val noNodeTakenDown = selectedNodes.none { node -> node.isTakenDown }
        return options
            .filter {
                it.shouldDisplay(
                    hasNodeAccessPermission = hasNodeAccessPermission,
                    selectedNodes = selectedNodes,
                    canBeMovedToTarget = allNodeCanBeMovedToTarget,
                    noNodeInBackups = noNodeInBackups,
                    noNodeTakenDown = noNodeTakenDown,
                    nodeSourceType = nodeSourceType,
                )
            }.map { option ->
                NodeSelectionModeMenuItem(
                    action = option.menuAction,
                    handler = option.buildHandler(selectedNodes),
                    showAsActionOrder = option.showAsActionOrder
                )
            }
    }
}
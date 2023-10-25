package mega.privacy.android.app.presentation.node.model.mapper

import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.NodeToolbarMenuItem
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Node toolbar action mapper
 */
class NodeToolbarActionMapper @Inject constructor() {

    /**
     * Invocation
     *
     * @param toolbarOptions all the toolbar options available for slected screen
     * @param selectedNodes selected nodes
     * @param resultCount total number of items in the list
     * @param hasNodeAccessPermission checks if node has rename permission
     * @param noNodeInBackups checks if no node is part of back ups
     * @param allNodeCanBeMovedToTarget checks if node can be moved to rubbish bin
     */
    operator fun invoke(
        toolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>>,
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        allNodeCanBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        resultCount: Int,
    ): List<MenuAction> {
        val noNodeTakenDown = selectedNodes.none { node -> node.isTakenDown }
        val allFileNodes = selectedNodes.all { node -> node is FileNode }
        return toolbarOptions
            .filter {
                it.shouldDisplay(
                    hasNodeAccessPermission = hasNodeAccessPermission,
                    selectedNodes = selectedNodes,
                    canBeMovedToTarget = allNodeCanBeMovedToTarget,
                    noNodeInBackups = noNodeInBackups,
                    noNodeTakenDown = noNodeTakenDown,
                    allFileNodes = allFileNodes,
                    resultCount = resultCount,
                )
            }.map { it.menuAction }

    }
}
package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.LeaveShareMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class LeaveShareSelectionMenuItem @Inject constructor(
    override val menuAction: LeaveShareMenuAction,
) : NodeSelectionMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
    ): Boolean = noNodeTakenDown && selectedNodes.run {
        isNotEmpty() && all { it is FolderNode && it.isIncomingShare }
    }
}

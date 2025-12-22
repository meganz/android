package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.GetLinkMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class GetLinkSelectionMenuItem @Inject constructor(
    override val menuAction: GetLinkMenuAction
) : NodeSelectionMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        nodeSourceType: NodeSourceType,
    ): Boolean = if (nodeSourceType == NodeSourceType.LINKS && selectedNodes.size > 1) {
        false
    } else {
        hasNodeAccessPermission && noNodeTakenDown && noNodeInBackups
                && !(selectedNodes.size == 1 && selectedNodes.first().exportedData != null)
    }

    override val showAsActionOrder: Int?
        get() = 110
}

package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.extension.isNotS4Container
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class MoveSelectionMenuItem @Inject constructor(
    override val menuAction: MoveMenuAction,
) : NodeSelectionMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        nodeSourceType: NodeSourceType,
    ): Boolean =
        hasNodeAccessPermission &&
                noNodeInBackups &&
                selectedNodes.run {
                    none { it.isIncomingShare } &&
                            all { it.isNotS4Container() }
                }

    override val showAsActionOrder: Int
        get() = 160
}
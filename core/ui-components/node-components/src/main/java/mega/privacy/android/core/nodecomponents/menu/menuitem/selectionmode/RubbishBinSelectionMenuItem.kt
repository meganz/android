package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class RubbishBinSelectionMenuItem @Inject constructor(
    override val menuAction: TrashMenuAction,
) : NodeSelectionMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
    ): Boolean =
        noNodeInBackups &&
                canBeMovedToTarget &&
                hasNodeAccessPermission &&
                !selectedNodes.any { it.isIncomingShare }
}
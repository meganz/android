package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.TrashMenuAction
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Rubbish bin menu item
 *
 * @property menuAction [TrashMenuAction]
 */
class Trash @Inject constructor(
    override val menuAction: TrashMenuAction,
) : NodeToolbarMenuItem<MenuAction> {


    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = noNodeInBackups && canBeMovedToTarget &&
            hasNodeAccessPermission && !selectedNodes.any { it.isIncomingShare }

}
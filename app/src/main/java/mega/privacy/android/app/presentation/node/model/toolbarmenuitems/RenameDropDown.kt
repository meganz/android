package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.RenameMenuAction
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Rename menu item
 *
 * This item will always be placed on the extras/more option
 */
class RenameDropDown @Inject constructor() : NodeToolbarMenuItem<MenuAction> {

    override val menuAction: RenameMenuAction = RenameMenuAction(220)

    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = hasNodeAccessPermission
            && selectedNodes.size == 1
            && noNodeInBackups

}
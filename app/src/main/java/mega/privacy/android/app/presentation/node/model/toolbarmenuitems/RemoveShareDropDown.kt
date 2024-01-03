package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.extensions.isOutShare
import mega.privacy.android.app.presentation.node.model.menuaction.RemoveShareMenuAction
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Remove share drop down menu item
 *
 * This item will always be placed on the extras/more option
 */
class RemoveShareDropDown @Inject constructor() : NodeToolbarMenuItem<MenuAction> {

    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ): Boolean = selectedNodes.isNotEmpty() && selectedNodes.all { it.isOutShare() }

    override val menuAction = RemoveShareMenuAction(210)

}
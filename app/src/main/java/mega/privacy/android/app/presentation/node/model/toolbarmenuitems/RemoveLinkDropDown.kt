package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.RemoveLinkMenuAction
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Remove link menu item
 *
 * This item will always be placed on the extras/more option
 */
class RemoveLinkDropDown @Inject constructor() : NodeToolbarMenuItem<MenuAction> {

    override val menuAction = RemoveLinkMenuAction(30)

    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = noNodeTakenDown
            && selectedNodes.size == 1
            && selectedNodes.first().exportedData != null
}
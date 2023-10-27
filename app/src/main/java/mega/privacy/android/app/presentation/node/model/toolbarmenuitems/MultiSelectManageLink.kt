package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Manage link menu item
 *
 * This is a special case of manage link where we show manage link if
 *      only one item selected and link is created for it
 *      or multiple items selected
 *      its only available from cloud drive screen
 */
class MultiSelectManageLink @Inject constructor() : NodeToolbarMenuItem<MenuActionWithIcon> {

    override val menuAction = ManageLinkMenuAction(160)
    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = noNodeTakenDown
            && hasNodeAccessPermission
            && ((selectedNodes.size == 1 && selectedNodes.first().exportedData != null) || selectedNodes.size > 1) //if size 1 and exported data null we show GetLink

}

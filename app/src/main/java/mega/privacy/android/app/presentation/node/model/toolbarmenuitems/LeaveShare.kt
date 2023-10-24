package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.LeaveShareMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Leave share menu item
 *
 * Handles leave share option from incoming shares. checks for isIncomingShare
 * to identify if the selected link is not some inner folders of shared folder
 *
 * @property menuAction [LeaveShareMenuAction]
 */
class LeaveShare @Inject constructor(
    override val menuAction: LeaveShareMenuAction,
) : NodeToolbarMenuItem<MenuActionWithIcon> {

    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = noNodeTakenDown
            && selectedNodes.all { it.isIncomingShare }

}
package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Manage link menu item
 */
class ManageLink @Inject constructor() : NodeToolbarMenuItem<MenuActionWithIcon> {

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
            && selectedNodes.size == 1
            && selectedNodes.first().exportedData != null //if size 1 and exported data null we show GetLink

}

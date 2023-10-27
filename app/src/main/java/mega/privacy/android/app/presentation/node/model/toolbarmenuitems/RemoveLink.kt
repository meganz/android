package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.RemoveLinkMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Remove link
 *
 * Handles remove link option from links screen
 * It does not check if link available data as all items in links screen should have links data
 */
class RemoveLink @Inject constructor() : NodeToolbarMenuItem<MenuActionWithIcon> {

    override val menuAction: RemoveLinkMenuAction = RemoveLinkMenuAction(170)
    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = noNodeTakenDown
            && selectedNodes.size > 1

}
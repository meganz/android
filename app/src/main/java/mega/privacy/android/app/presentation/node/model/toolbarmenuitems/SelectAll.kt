package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.SelectAllMenuAction
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Select all menu item
 *
 * This item will always be placed on the extras/more option
 * @property menuAction [SelectAllMenuAction]
 */
class SelectAll @Inject constructor(
    override val menuAction: SelectAllMenuAction,
) : NodeToolbarMenuItem<MenuAction> {

    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = selectedNodes.size != resultCount

}
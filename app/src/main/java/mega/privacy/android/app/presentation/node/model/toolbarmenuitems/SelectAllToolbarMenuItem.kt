package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.SelectAllMenuAction
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Select all menu item
 *
 * This item will always be placed on the extras/more option
 * @property menuAction [SelectAllMenuAction]
 */
class SelectAllToolbarMenuItem @Inject constructor(
    override val menuAction: SelectAllMenuAction,
) : NodeToolbarMenuItem<MenuAction> {

    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = selectedNodes.size != resultCount

}
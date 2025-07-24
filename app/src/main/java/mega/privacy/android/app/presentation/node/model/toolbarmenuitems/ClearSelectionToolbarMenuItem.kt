package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.ClearSelectionMenuAction
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Clear selection menu item
 *
 * This item will always be placed on the extras/more option
 * @property menuAction [ClearSelectionMenuAction]
 */
class ClearSelectionToolbarMenuItem @Inject constructor(
    override val menuAction: ClearSelectionMenuAction,
) : NodeToolbarMenuItem<MenuAction> {

    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = selectedNodes.isNotEmpty()

}
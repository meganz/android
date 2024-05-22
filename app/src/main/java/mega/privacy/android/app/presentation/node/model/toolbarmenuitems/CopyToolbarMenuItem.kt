package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.CopyMenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Copy menu item
 *
 * @property menuAction [CopyMenuAction]
 */
class CopyToolbarMenuItem @Inject constructor(
    override val menuAction: CopyMenuAction,
) : NodeToolbarMenuItem<MenuActionWithIcon> {

    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = noNodeTakenDown

}
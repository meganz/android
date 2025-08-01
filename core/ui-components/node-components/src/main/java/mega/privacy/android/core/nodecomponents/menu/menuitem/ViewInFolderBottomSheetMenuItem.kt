package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.entity.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuaction.ViewInFolderMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * View in folder bottom sheet menu item
 *
 * @param menuAction [ViewInFolderMenuAction]
 */
class ViewInFolderBottomSheetMenuItem @Inject constructor(
    override val menuAction: ViewInFolderMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = false


    override val groupId = 3
}
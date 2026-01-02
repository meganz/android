package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.OpenWithMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Open with bottom sheet menu item
 *
 * @param menuAction [OpenWithMenuAction]
 */
class OpenWithBottomSheetMenuItem @Inject constructor(
    override val menuAction: OpenWithMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node is TypedFileNode
            && node.isTakenDown.not()
            && isNodeInRubbish.not() && node.isNodeKeyDecrypted

    override val groupId = 5
}
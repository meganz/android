package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.extension.isNotS4Container
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler

/**
 * Move bottom sheet menu item
 *
 * @param menuAction [MoveMenuAction]
 */
class MoveBottomSheetMenuItem @Inject constructor(
    override val menuAction: MoveMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isInBackups.not()
            && isNodeInRubbish.not()
            && node.isIncomingShare.not()
            && accessPermission in listOf(AccessPermission.OWNER, AccessPermission.FULL)
            && node.isNotS4Container()

    override val groupId: Int
        get() = 8
}
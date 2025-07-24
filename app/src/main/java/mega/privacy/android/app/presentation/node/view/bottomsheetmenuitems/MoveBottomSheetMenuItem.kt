package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.MoveMenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

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
            && accessPermission in listOf(
        AccessPermission.OWNER,
        AccessPermission.FULL,
    )

    override val groupId: Int
        get() = 8
}
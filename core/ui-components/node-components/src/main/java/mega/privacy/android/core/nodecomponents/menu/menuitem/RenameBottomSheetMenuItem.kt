package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.RenameMenuAction
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Rename bottom sheet menu item
 */
class RenameBottomSheetMenuItem @Inject constructor(
    override val menuAction: RenameMenuAction,
) :
    NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isNodeInRubbish.not() && accessPermission in listOf(
        AccessPermission.OWNER,
        AccessPermission.FULL
    ) && isInBackups.not()

    override fun getOnClickFunction(
        node: TypedNode,
        handler: BottomSheetClickHandler,
    ): () -> Unit = {
        handler.actionHandler(menuAction, node)
        handler.onDismiss()
    }

    override val groupId = 8
}
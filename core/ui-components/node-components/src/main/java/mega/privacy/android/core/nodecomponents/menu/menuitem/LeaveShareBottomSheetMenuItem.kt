package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.dialog.leaveshare.LeaveShareDialogArgs
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.LeaveShareMenuAction
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Leave share bottom sheet menu item
 *
 * @param menuAction [LeaveShareMenuAction]
 */
class LeaveShareBottomSheetMenuItem @Inject constructor(
    override val menuAction: LeaveShareMenuAction,
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && node.isIncomingShare
            && isInBackups.not()

    override fun getOnClickFunction(
        node: TypedNode,
        handler: BottomSheetClickHandler
    ): () -> Unit = {
        runCatching {
            nodeHandlesToJsonMapper(listOf(node.id.longValue))
        }.onSuccess {
            handler.navigationHandler.navigate(
                LeaveShareDialogArgs(handles = it)
            )
        }
        handler.onDismiss()
    }

    override val isDestructiveAction: Boolean
        get() = true
    override val groupId: Int
        get() = 9
}
package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.dialog.delete.MoveToRubbishOrDeleteDialogArgs
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.extension.isNotS4Container
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import timber.log.Timber
import javax.inject.Inject

/**
 * Trash bottom sheet menu item
 *
 * @param menuAction [TrashMenuAction]
 * @param nodeHandlesToJsonMapper [NodeHandlesToJsonMapper]
 */
class TrashBottomSheetMenuItem @Inject constructor(
    override val menuAction: TrashMenuAction,
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isNodeInRubbish.not()
            && node.isIncomingShare.not()
            && accessPermission in listOf(
        AccessPermission.OWNER,
        AccessPermission.FULL,
    ) && isInBackups.not()
            && node.isNotS4Container() && node.isNodeKeyDecrypted

    override val isDestructiveAction: Boolean
        get() = true

    override val groupId = 9
}
package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.extension.isNotS4Container
import mega.privacy.android.core.nodecomponents.menu.menuaction.GetLinkMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Get link bottom sheet menu action
 *
 * @param menuAction [GetLinkMenuAction]
 */
class GetLinkBottomSheetMenuItem @Inject constructor(
    override val menuAction: GetLinkMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && node.exportedData?.publicLink.isNullOrEmpty()
            && isNodeInRubbish.not()
            && accessPermission == AccessPermission.OWNER
            && node.isNotS4Container() && node.isNodeKeyDecrypted

    override val groupId: Int
        get() = 7
}
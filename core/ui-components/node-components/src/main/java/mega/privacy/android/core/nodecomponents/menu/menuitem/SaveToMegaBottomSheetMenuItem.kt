package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.extension.isNotS4Container
import mega.privacy.android.core.nodecomponents.menu.menuaction.SaveToMegaMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Save to MEGA (import) bottom sheet menu action, used in public link screens
 *
 * @param menuAction [SaveToMegaMenuAction]
 */
class SaveToMegaBottomSheetMenuItem @Inject constructor(
    override val menuAction: SaveToMegaMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
        nodeSourceType: NodeSourceType,
    ) =
        node.isTakenDown.not() && isNodeInRubbish.not() && node.isNotS4Container() && node.isNodeKeyDecrypted

    override val groupId = 6
}
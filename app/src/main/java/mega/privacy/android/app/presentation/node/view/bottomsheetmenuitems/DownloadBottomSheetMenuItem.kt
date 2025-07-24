package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.DownloadMenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Download bottom sheet menu item
 *
 * @param menuAction [DownloadMenuAction]
 */
class DownloadBottomSheetMenuItem @Inject constructor(
    override val menuAction: DownloadMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not() && isNodeInRubbish.not()

    override val groupId = 6
}
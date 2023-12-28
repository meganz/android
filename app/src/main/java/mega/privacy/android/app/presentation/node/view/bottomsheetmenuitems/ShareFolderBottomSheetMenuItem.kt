package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Share folder bottom sheet menu item
 *
 * @param menuAction [ShareFolderMenuAction]
 */
class ShareFolderBottomSheetMenuItem @Inject constructor(
    override val menuAction: ShareFolderMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && node is TypedFolderNode
            && isNodeInRubbish.not()
            && isOutShare(node)

    private fun isOutShare(node: TypedNode) = if (node is FolderNode) {
        node.isPendingShare || node.isShared
    } else {
        false
    }

    override val groupId = 7
}
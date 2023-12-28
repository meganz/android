package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.RemoveShareMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Remove share bottom sheet menu item
 */
class RemoveShareBottomSheetMenuItem @Inject constructor() :
    NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && isOutShare(node)
            && isNodeInRubbish.not()

    private fun isOutShare(node: TypedNode) = if (node is FolderNode) {
        node.isPendingShare || node.isShared
    } else {
        false
    }

    override val menuAction = RemoveShareMenuAction(210)
    override val groupId = 7
}
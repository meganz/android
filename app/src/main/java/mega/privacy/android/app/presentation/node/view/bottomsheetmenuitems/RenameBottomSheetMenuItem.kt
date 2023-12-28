package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.RenameMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Rename bottom sheet menu item
 */
class RenameBottomSheetMenuItem @Inject constructor() :
    NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isNodeInRubbish.not()
            && accessPermission in listOf(AccessPermission.OWNER, AccessPermission.FULL)
            && isInBackups.not()

    override val menuAction = RenameMenuAction(220)
    override val groupId = 8
}
package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.VerifyMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Verify bottom sheet menu item
 *
 * @param menuAction [VerifyMenuAction]
 */
class VerifyBottomSheetMenuItem @Inject constructor(
    override val menuAction: VerifyMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = false

    override val groupId = 2
}
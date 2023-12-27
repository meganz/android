package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.GetLinkMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
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
    override fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && node.exportedData?.publicLink.isNullOrEmpty()
            && isNodeInRubbish.not()
            && accessPermission == AccessPermission.OWNER

    override val groupId: Int
        get() = 7
}
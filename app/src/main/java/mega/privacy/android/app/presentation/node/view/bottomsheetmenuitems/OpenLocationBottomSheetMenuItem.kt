package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.OpenLocationMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Open location bottom sheet menu item
 *
 * @param menuAction [OpenLocationMenuAction]
 */
class OpenLocationBottomSheetMenuItem @Inject constructor(
    override val menuAction: OpenLocationMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isInBackups.not()

    override val groupId = 5
}
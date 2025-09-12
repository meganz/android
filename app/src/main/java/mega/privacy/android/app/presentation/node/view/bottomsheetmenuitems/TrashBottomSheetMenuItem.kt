package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.app.presentation.node.model.menuaction.TrashMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import javax.inject.Inject

/**
 * Trash bottom sheet menu item
 *
 * @param menuAction [TrashMenuAction]
 * @param listToStringWithDelimitersMapper [ListToStringWithDelimitersMapper]
 */
class TrashBottomSheetMenuItem @Inject constructor(
    override val menuAction: TrashMenuAction,
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
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

    override val isDestructiveAction: Boolean
        get() = true

    override val groupId = 9
}
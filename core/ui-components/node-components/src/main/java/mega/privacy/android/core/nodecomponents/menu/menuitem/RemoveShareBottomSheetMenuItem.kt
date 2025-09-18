package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveShareMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.shares.IsOutShareUseCase
import javax.inject.Inject

/**
 * Remove share bottom sheet menu item
 */
class RemoveShareBottomSheetMenuItem @Inject constructor(
    private val isOutShareUseCase: IsOutShareUseCase
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && isOutShareUseCase(node)
            && isNodeInRubbish.not()

    override val menuAction = RemoveShareMenuAction(210)

    override val groupId = 7
}
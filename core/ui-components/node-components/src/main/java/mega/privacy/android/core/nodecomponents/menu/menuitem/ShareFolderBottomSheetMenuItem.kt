package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.extension.isNotS4Container
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.shares.IsOutShareUseCase
import javax.inject.Inject

/**
 * Share folder bottom sheet menu item
 *
 * @param menuAction [ShareFolderMenuAction]
 */
class ShareFolderBottomSheetMenuItem @Inject constructor(
    override val menuAction: ShareFolderMenuAction,
    private val isOutShareUseCase: IsOutShareUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && node is TypedFolderNode
            && isNodeInRubbish.not()
            && isOutShareUseCase(node).not()
            && accessPermission == AccessPermission.OWNER
            && node.isNotS4Container()

    override fun getOnClickFunction(
        node: TypedNode,
        handler: BottomSheetClickHandler,
    ): () -> Unit = {
        handler.actionHandler(menuAction, node)
    }

    override val groupId = 7
}
package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveOfflineMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.foldernode.IsFolderEmptyUseCase
import javax.inject.Inject

class RemoveAvailableOfflineBottomSheetMenuItem @Inject constructor(
    override val menuAction: RemoveOfflineMenuAction,
    private val isFolderEmptyUseCase: IsFolderEmptyUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isAvailableOffline &&
            isNodeInRubbish.not() &&
            node.isTakenDown.not() &&
            isFolderEmptyUseCase(node).not()

    override val groupId = 6
}
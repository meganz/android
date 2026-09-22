package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.extension.isAvailableOffline
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveOfflineMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.OfflineTypedNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.foldernode.IsFolderEmptyUseCase
import mega.privacy.android.domain.usecase.offline.IsNodeUnderOfflineRootUseCase
import javax.inject.Inject

class RemoveAvailableOfflineBottomSheetMenuItem @Inject constructor(
    override val menuAction: RemoveOfflineMenuAction,
    private val isFolderEmptyUseCase: IsFolderEmptyUseCase,
    private val isNodeUnderOfflineRootUseCase: IsNodeUnderOfflineRootUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
        nodeSourceType: NodeSourceType,
    ) = node.isAvailableOffline(isNodeUnderOfflineRootUseCase) &&
            isNodeInRubbish.not() &&
            node.isTakenDown.not() &&
            (node is OfflineTypedNode || isFolderEmptyUseCase(node).not())

    override val groupId = 6
}

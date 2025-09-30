package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.shares.IsOutShareUseCase
import javax.inject.Inject

class ShareFolderSelectionMenuItem @Inject constructor(
    override val menuAction: ShareFolderMenuAction,
    private val isOutShareUseCase: IsOutShareUseCase
) : NodeSelectionMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
    ): Boolean = noNodeTakenDown && selectedNodes.run {
        isNotEmpty() && all { it is FolderNode } && any { isOutShareUseCase(it) }
    }
}

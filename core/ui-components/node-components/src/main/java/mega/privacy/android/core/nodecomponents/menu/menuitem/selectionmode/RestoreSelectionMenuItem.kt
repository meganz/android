package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.RestoreMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import javax.inject.Inject

class RestoreSelectionMenuItem @Inject constructor(
    override val menuAction: RestoreMenuAction,
    private val isNodeDeletedFromBackupsUseCase: IsNodeDeletedFromBackupsUseCase,
) : NodeSelectionMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
    ): Boolean = selectedNodes.isNotEmpty()
            && noNodeTakenDown
            && (selectedNodes.all { it.restoreId != null }
            || runCatching { isNodeDeletedFromBackupsUseCase(selectedNodes.first().parentId) }
        .getOrDefault(false))
}
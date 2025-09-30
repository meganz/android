package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveShareMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.shares.IsOutShareUseCase
import javax.inject.Inject

class RemoveShareSelectionMenuItem @Inject constructor(
    override val menuAction: RemoveShareMenuAction,
    private val isOutShareUseCase: IsOutShareUseCase
) : NodeSelectionMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
    ): Boolean = selectedNodes.run {
        isNotEmpty() && all { isOutShareUseCase(it) }
    }
}

package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.extension.isAvailableOffline
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveOfflineMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.offline.IsNodeUnderOfflineRootUseCase
import javax.inject.Inject

class RemoveOfflineSelectionMenuItem @Inject constructor(
    override val menuAction: RemoveOfflineMenuAction,
    private val isNodeUnderOfflineRootUseCase: IsNodeUnderOfflineRootUseCase,
) : NodeSelectionMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        nodeSourceType: NodeSourceType,
    ): Boolean =
        noNodeTakenDown && selectedNodes.isNotEmpty() &&
                selectedNodes.all { it.isAvailableOffline(isNodeUnderOfflineRootUseCase) }

    override val showAsActionOrder: Int?
        get() = null
}

package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.FavouriteMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Selection mode menu item for adding selected nodes to favourites.
 * Shown when at least one selected node is not a favourite.
 */
class AddToFavouritesSelectionMenuItem @Inject constructor(
    override val menuAction: FavouriteMenuAction,
) : NodeSelectionMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        nodeSourceType: NodeSourceType,
    ): Boolean {
        if (!noNodeTakenDown || selectedNodes.isEmpty()) return false
        return selectedNodes.any { !it.isFavourite }
    }

    override val showAsActionOrder: Int?
        get() = 180
}

package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveFavouriteMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Selection mode menu item for removing selected nodes from favourites.
 * Shown when [ApiFeatures.AllowMultipleSelectionsEnabled] is on and all selected nodes are favourites.
 */
class RemoveFromFavouritesSelectionMenuItem @Inject constructor(
    override val menuAction: RemoveFavouriteMenuAction,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
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
        val isMultipleSelectionEnabled = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.AllowMultipleSelectionsEnabled)
        }.onFailure { Timber.w(it, "Multiple selection flag check failed") }.getOrElse { false }
        if (!isMultipleSelectionEnabled) return false
        return selectedNodes.all { it.isFavourite }
    }

    override val showAsActionOrder: Int?
        get() = 180
}

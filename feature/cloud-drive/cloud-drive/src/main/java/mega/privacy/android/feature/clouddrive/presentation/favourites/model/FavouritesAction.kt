package mega.privacy.android.feature.clouddrive.presentation.favourites.model

import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Favourites action
 * This interface defines the UI actions that can be performed in the Favourites screen.
 */
sealed interface FavouritesAction {
    /**
     * Item clicked action
     */
    data class ItemClicked(val nodeUiItem: NodeUiItem<TypedNode>) : FavouritesAction

    /**
     * Item long clicked action
     */
    data class ItemLongClicked(val nodeUiItem: NodeUiItem<TypedNode>) : FavouritesAction

    /**
     * Change view type clicked action
     */
    data object ChangeViewTypeClicked : FavouritesAction

    /**
     * Opened file node handled action
     */
    data object OpenedFileNodeHandled : FavouritesAction

    /**
     * Select all items action
     */
    data object SelectAllItems : FavouritesAction

    /**
     * Deselect all items action
     */
    data object DeselectAllItems : FavouritesAction

    /**
     * Navigate to folder event consumed action
     */
    data object NavigateToFolderEventConsumed : FavouritesAction
}

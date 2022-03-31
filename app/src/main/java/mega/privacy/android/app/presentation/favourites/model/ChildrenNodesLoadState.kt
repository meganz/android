package mega.privacy.android.app.presentation.favourites.model

/**
 * The children nodes load state
 */
sealed interface ChildrenNodesLoadState {
    /**
     * Get children nodes success
     * @param children List<FavouriteItemUIO>
     */
    data class Success(val title: String, val children: List<Favourite>): ChildrenNodesLoadState

    /**
     * Loading state
     */
    object Loading: ChildrenNodesLoadState

    /**
     * The children nodes is empty
     */
    data class Empty(val title: String): ChildrenNodesLoadState
}
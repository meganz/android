package mega.privacy.android.app.presentation.favourites.model

/**
 * The children nodes load state
 */
sealed interface ChildrenNodesLoadState {
    /**
     * Get children nodes success
     * @param children List<FavouriteItem>
     */
    data class Success(val title: String, val children: List<FavouriteItem>): ChildrenNodesLoadState

    /**
     * Loading state
     */
    object Loading: ChildrenNodesLoadState

    /**
     * The children nodes is empty
     * @param title title of current folder
     */
    data class Empty(val title: String?): ChildrenNodesLoadState
}
package mega.privacy.android.app.presentation.favourites.model

/**
 * The children nodes load state
 */
sealed interface ChildrenNodesLoadState {
    /**
     * Get children nodes success
     *
     * @param title page title
     * @param children List<FavouriteItem>
     * @param isBackPressedEnable ture is enable back pressed, otherwise is false
     */
    data class Success(
        val title: String,
        val children: List<FavouriteItem>,
        val isBackPressedEnable: Boolean,
    ) :
        ChildrenNodesLoadState

    /**
     * Loading state
     */
    object Loading : ChildrenNodesLoadState

    /**
     * The children nodes is empty
     * @param title title of current folder
     */
    data class Empty(val title: String?) : ChildrenNodesLoadState
}
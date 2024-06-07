package mega.privacy.android.app.presentation.favourites.model

import mega.privacy.android.domain.entity.AccountType

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
     * @param accountType AccountType
     */
    data class Success(
        val title: String,
        val children: List<FavouriteItem>,
        val isBackPressedEnable: Boolean,
        val accountType: AccountType? = null,
    ) :
        ChildrenNodesLoadState

    /**
     * Loading state
     */
    data object Loading : ChildrenNodesLoadState

    /**
     * The children nodes is empty
     * @param title title of current folder
     */
    data class Empty(val title: String?) : ChildrenNodesLoadState
}
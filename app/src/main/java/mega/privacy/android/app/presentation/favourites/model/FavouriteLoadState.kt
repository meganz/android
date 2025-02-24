package mega.privacy.android.app.presentation.favourites.model

import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.NodeId

/**
 * The favourite list load state
 */
sealed interface FavouriteLoadState {
    val isConnected: Boolean

    /**
     * Get favourite list success
     *
     * @param favourites FavouriteItem list
     * @param selectedItems selected items
     * @param accountType account type
     * @param isBusinessAccountExpired if the business account is expired
     * @param isHiddenNodesOnboarded if is hidden nodes onboarded
     * @param hiddenNodeEnabled if hidden node is enabled
     * @param isConnected is connected
     */
    data class Success(
        val favourites: List<FavouriteItem>,
        val selectedItems: Set<NodeId>,
        val accountType: AccountType? = null,
        val isBusinessAccountExpired: Boolean = false,
        val isHiddenNodesOnboarded: Boolean? = null,
        val hiddenNodeEnabled: Boolean = false,
        override val isConnected: Boolean,
    ) :
        FavouriteLoadState

    /**
     * Loading state
     */
    data class Loading(
        override val isConnected: Boolean,
    ) : FavouriteLoadState

    /**
     * Favourite list is empty
     */
    data class Empty(
        override val isConnected: Boolean,
    ) : FavouriteLoadState

    /**
     * Get favourite list error
     * @param exception MegaException
     */
    data class Error(
        val exception: MegaException,
        override val isConnected: Boolean,
    ) : FavouriteLoadState
}
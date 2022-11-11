package mega.privacy.android.app.presentation.favourites.model

import mega.privacy.android.app.usecase.exception.MegaException

/**
 * The favourite list load state
 */
sealed interface FavouriteLoadState {
    val showSearch: Boolean

    /**
     * Get favourite list success
     * @param favourites FavouriteItem list
     */
    data class Success(val favourites: List<FavouriteItem>, override val showSearch: Boolean) :
        FavouriteLoadState

    /**
     * Loading state
     */
    data class Loading(override val showSearch: Boolean) : FavouriteLoadState

    /**
     * Favourite list is empty
     */
    data class Empty(override val showSearch: Boolean) : FavouriteLoadState

    /**
     * Get favourite list error
     * @param exception MegaException
     */
    data class Error(val exception: MegaException, override val showSearch: Boolean) :
        FavouriteLoadState
}
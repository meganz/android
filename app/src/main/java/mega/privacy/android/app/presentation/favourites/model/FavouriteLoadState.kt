package mega.privacy.android.app.presentation.favourites.model

import mega.privacy.android.app.usecase.MegaException

/**
 * The favourite list load state
 */
sealed interface FavouriteLoadState {
    /**
     * Get favourite list success
     * @param favourites FavouriteItem list
     */
    data class Success(val favourites: List<FavouriteItem>): FavouriteLoadState

    /**
     * Loading state
     */
    object Loading: FavouriteLoadState

    /**
     * Favourite list is empty
     */
    object Empty: FavouriteLoadState

    /**
     * Get favourite list error
     * @param exception MegaException
     */
    data class Error(val exception: MegaException): FavouriteLoadState
}
package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder

/**
 * Get favourite sort order
 */
fun interface GetFavouriteSortOrder {

    /**
     * Invoke
     *
     * @return Current sort order for favourites
     */
    suspend operator fun invoke(): FavouriteSortOrder
}
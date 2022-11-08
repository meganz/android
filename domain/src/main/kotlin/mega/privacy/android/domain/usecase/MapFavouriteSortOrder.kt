package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder

/**
 * Get favourite sort order for sort order
 */
fun interface MapFavouriteSortOrder {
    /**
     * Invoke
     *
     * @param sortOrder
     * @return Favourite sort order
     */
    operator fun invoke(sortOrder: SortOrder): FavouriteSortOrder
}
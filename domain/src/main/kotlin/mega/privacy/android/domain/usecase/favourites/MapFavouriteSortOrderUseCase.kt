package mega.privacy.android.domain.usecase.favourites

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder
import javax.inject.Inject

/**
 * Get favourite sort order for sort order
 */
class MapFavouriteSortOrderUseCase @Inject constructor() {
    /**
     * Invoke
     *
     * @param sortOrder
     * @return Favourite sort order
     */
    operator fun invoke(sortOrder: SortOrder) = when (sortOrder) {
        SortOrder.ORDER_DEFAULT_ASC -> FavouriteSortOrder.Name(false)
        SortOrder.ORDER_DEFAULT_DESC -> FavouriteSortOrder.Name(true)
        SortOrder.ORDER_SIZE_ASC -> FavouriteSortOrder.Size(false)
        SortOrder.ORDER_SIZE_DESC -> FavouriteSortOrder.Size(true)
        SortOrder.ORDER_MODIFICATION_ASC -> FavouriteSortOrder.ModifiedDate(false)
        SortOrder.ORDER_MODIFICATION_DESC -> FavouriteSortOrder.ModifiedDate(true)
        SortOrder.ORDER_CREATION_ASC -> FavouriteSortOrder.AddedDate(false)
        SortOrder.ORDER_CREATION_DESC -> FavouriteSortOrder.AddedDate(true)
        SortOrder.ORDER_LABEL_ASC -> FavouriteSortOrder.Label(false)
        SortOrder.ORDER_LABEL_DESC -> FavouriteSortOrder.Label(true)
        else -> FavouriteSortOrder.Name(false)
    }

}
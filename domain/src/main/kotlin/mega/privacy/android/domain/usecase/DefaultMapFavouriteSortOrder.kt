package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder
import javax.inject.Inject

/**
 * Default map favourite sort order
 */
class DefaultMapFavouriteSortOrder @Inject constructor() : MapFavouriteSortOrder {
    override fun invoke(sortOrder: SortOrder) = when (sortOrder) {
        SortOrder.ORDER_DEFAULT_ASC -> FavouriteSortOrder.Name(false)
        SortOrder.ORDER_DEFAULT_DESC -> FavouriteSortOrder.Name(true)
        SortOrder.ORDER_SIZE_ASC -> FavouriteSortOrder.Size(false)
        SortOrder.ORDER_SIZE_DESC -> FavouriteSortOrder.Size(true)
        SortOrder.ORDER_MODIFICATION_ASC -> FavouriteSortOrder.ModifiedDate(false)
        SortOrder.ORDER_MODIFICATION_DESC -> FavouriteSortOrder.ModifiedDate(true)
        else -> FavouriteSortOrder.Label
    }

}
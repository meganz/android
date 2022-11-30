package mega.privacy.android.app.presentation.favourites.model.mapper

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.favourites.model.FavouriteHeaderItem
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder

typealias HeaderMapper = (@JvmSuppressWildcards FavouriteSortOrder) -> @JvmSuppressWildcards FavouriteHeaderItem

internal fun toHeader(sortOrder: FavouriteSortOrder) = FavouriteHeaderItem(
    favourite = null,
    orderStringId = getFavouriteSortHeaderStringIdentifier(sortOrder)
)

private fun getFavouriteSortHeaderStringIdentifier(order: FavouriteSortOrder) = when (order) {
    FavouriteSortOrder.Label -> R.string.title_label
    is FavouriteSortOrder.ModifiedDate -> R.string.sortby_date
    is FavouriteSortOrder.Name -> R.string.sortby_name
    is FavouriteSortOrder.Size -> R.string.sortby_size
}
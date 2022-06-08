package mega.privacy.android.app.presentation.favourites.model

import mega.privacy.android.app.utils.Constants.ITEM_VIEW_TYPE

/**
 * Favourite list item for displaying favourite item
 */
data class FavouriteListItem(
    override val favourite: Favourite,
    override val forceUpdate: Boolean = false,
) : FavouriteItem {
    override val type = ITEM_VIEW_TYPE
}
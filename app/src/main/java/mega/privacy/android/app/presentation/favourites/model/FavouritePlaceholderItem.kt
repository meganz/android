package mega.privacy.android.app.presentation.favourites.model

import mega.privacy.android.app.utils.Constants.ITEM_PLACEHOLDER_TYPE

/**
 * Favourite placeholder item for switching list style.
 */
data class FavouritePlaceholderItem(
    override val favourite: Favourite? = null,
    override val forceUpdate: Boolean = false
): FavouriteItem {
    override val type = ITEM_PLACEHOLDER_TYPE
}

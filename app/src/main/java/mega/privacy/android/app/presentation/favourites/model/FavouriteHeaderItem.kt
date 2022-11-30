package mega.privacy.android.app.presentation.favourites.model

import mega.privacy.android.app.utils.Constants.HEADER_VIEW_TYPE

/**
 * Favourite header item for sorting order and switch list style.
 * @param orderStringId order strings id
 */
data class FavouriteHeaderItem(
    override val favourite: Favourite?,
    val orderStringId: Int?
): FavouriteItem {
    override val type = HEADER_VIEW_TYPE
}

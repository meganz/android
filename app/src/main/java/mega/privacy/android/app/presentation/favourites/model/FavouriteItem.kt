package mega.privacy.android.app.presentation.favourites.model

/**
 * The favourite item interface
 * @property favourite Favourite
 * @property type item type
 */
sealed interface FavouriteItem {
    val favourite: Favourite?
    val type: Int
}


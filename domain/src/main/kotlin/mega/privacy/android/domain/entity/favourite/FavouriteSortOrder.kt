package mega.privacy.android.domain.entity.favourite

/**
 * Favourite sort order
 */
sealed interface FavouriteSortOrder {
    /**
     * Sort descending
     */
    val sortDescending: Boolean

    /**
     * Name
     *
     * @property sortDescending
     */
    data class Name(override val sortDescending: Boolean) : FavouriteSortOrder

    /**
     * Size
     *
     * @property sortDescending
     */
    data class Size(override val sortDescending: Boolean) : FavouriteSortOrder

    /**
     * Modified date
     *
     * @property sortDescending
     */
    data class ModifiedDate(override val sortDescending: Boolean) : FavouriteSortOrder

    /**
     * Label
     */
    object Label : FavouriteSortOrder {
        override val sortDescending = false
    }
}
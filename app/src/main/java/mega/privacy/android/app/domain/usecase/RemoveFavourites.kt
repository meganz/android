package mega.privacy.android.app.domain.usecase

/**
 * Use case interface for removing favourites
 */
fun interface RemoveFavourites {

    /**
     * Removing favourites
     * @param handles the handle of items that are removed.
     */
    suspend operator fun invoke(handles: List<Long>)
}
package mega.privacy.android.app.presentation.favourites.model

/**
 * The state for clicked event
 */
sealed interface ClickEventState {
    /**
     * Open file
     * @param favouriteFile FavouriteFile
     */
    data class OpenFile(val favouriteFile: FavouriteFile): ClickEventState

    /**
     * Open folder
     * @param parentHandle parentHandle
     */
    data class OpenFolder(val parentHandle: Long): ClickEventState

    /**
     * Offline
     */
    object Offline: ClickEventState

    /**
     * Open bottom sheet fragment
     * @param favourite Favourite
     */
    data class OpenBottomSheetFragment(val favourite: Favourite): ClickEventState
}
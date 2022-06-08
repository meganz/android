package mega.privacy.android.app.presentation.favourites.model

/**
 * The state for clicked event
 */
sealed interface FavouritesEventState {
    /**
     * Open file
     * @param favouriteFile FavouriteFile
     */
    data class OpenFile(val favouriteFile: FavouriteFile): FavouritesEventState

    /**
     * Open folder
     * @param parentHandle parentHandle
     */
    data class OpenFolder(val parentHandle: Long): FavouritesEventState

    /**
     * Offline
     */
    object Offline: FavouritesEventState

    /**
     * Open bottom sheet fragment
     * @param favourite Favourite
     */
    data class OpenBottomSheetFragment(val favourite: Favourite): FavouritesEventState

    /**
     * Action Mode state
     * @param selectedCount the count of selected items
     */
    data class ActionModeState(val selectedCount: Int): FavouritesEventState
}
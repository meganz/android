package mega.privacy.android.app.presentation.photos.albums.albumcontent

import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.photos.Photo

/**
 * State of the Album Content screen.
 * @property isLoading True if the screen is loading, false otherwise.
 * @property isAddingPhotos True if the screen is adding photos, false otherwise.
 * @property totalAddedPhotos Total added photos.
 * @property isDeleteAlbum True if the screen is deleting the album, false otherwise.
 * @property isRemovingPhotos True if the screen is removing photos, false otherwise.
 * @property totalRemovedPhotos Total removed photos.
 * @property showRemoveLinkConfirmation True if the remove link confirmation is shown, false otherwise.
 * @property isLinkRemoved True if the link is removed, false otherwise.
 * @property uiAlbum The UIAlbum.
 * @property photos The list of photos.
 * @property selectedPhotos The set of selected photos.
 * @property currentSort The current sort.
 * @property currentMediaType The current media type.
 * @property snackBarMessage The snackbar message.
 * @property showRenameDialog True if the rename dialog is shown, false otherwise.
 * @property showSortByDialog True if the sort by dialog is shown, false otherwise.
 * @property showFilterDialog True if the filter dialog is shown, false otherwise.
 * @property showDeleteAlbumsConfirmation True if the delete albums confirmation is shown, false otherwise.
 * @property showRemovePhotosDialog True if the remove photos dialog is shown, false otherwise.
 * @property isInputNameValid True if the input name is valid, false otherwise.
 * @property createDialogErrorMessage The create dialog error message.
 * @property newAlbumTitleInput The new album title input.
 * @property accountType The account type.
 * @property isHiddenNodesOnboarded True if the hidden nodes are onboarded, false otherwise.
 * @property isBusinessAccountExpired True if the business account is expired, false otherwise.
 */
data class AlbumContentState(
    val isLoading: Boolean = true,
    val isAddingPhotos: Boolean = false,
    val totalAddedPhotos: Int = 0,
    val isDeleteAlbum: Boolean = false,
    val isRemovingPhotos: Boolean = false,
    val totalRemovedPhotos: Int = 0,
    val showRemoveLinkConfirmation: Boolean = false,
    val isLinkRemoved: Boolean = false,
    val uiAlbum: UIAlbum? = null,
    val photos: List<Photo> = listOf(),
    val selectedPhotos: Set<Photo> = emptySet(),
    val currentSort: Sort = Sort.NEWEST,
    val currentMediaType: FilterMediaType = FilterMediaType.ALL_MEDIA,
    val snackBarMessage: String = "",
    val showRenameDialog: Boolean = false,
    val showSortByDialog: Boolean = false,
    val showFilterDialog: Boolean = false,
    val showDeleteAlbumsConfirmation: Boolean = false,
    val showRemovePhotosDialog: Boolean = false,
    val isInputNameValid: Boolean = true,
    val createDialogErrorMessage: Int? = null,
    val newAlbumTitleInput: String = "",
    val accountType: AccountType? = null,
    val isHiddenNodesOnboarded: Boolean? = null,
    val isBusinessAccountExpired: Boolean = false,
) {
    val isAddingPhotosProgressCompleted: Boolean
        get() = !isAddingPhotos && totalAddedPhotos > 0
    val isRemovingPhotosProgressCompleted: Boolean
        get() = !isRemovingPhotos && totalRemovedPhotos > 0
}

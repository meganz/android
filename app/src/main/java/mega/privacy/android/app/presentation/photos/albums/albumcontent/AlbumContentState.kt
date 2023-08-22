package mega.privacy.android.app.presentation.photos.albums.albumcontent

import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.domain.entity.photos.Photo

data class AlbumContentState(
    val isLoading: Boolean = true,
    val isAddingPhotos: Boolean = false,
    val totalAddedPhotos: Int = 0,
    /** A temporary field to support compatibility between view and compose architecture. */
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
) {
    val isAddingPhotosProgressCompleted: Boolean
        get() = !isAddingPhotos && totalAddedPhotos > 0
    val isRemovingPhotosProgressCompleted: Boolean
        get() = !isRemovingPhotos && totalRemovedPhotos > 0
}

package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.domain.entity.photos.Album

/**
 * @property albums
 * @property currentAlbum
 * @property selectedPhotoIds
 * @property currentSort
 * @property currentMediaType
 * @property createAlbumPlaceholderTitle
 * @property isInputNameValid
 * @property createDialogErrorMessage
 * @property isAlbumCreatedSuccessfully
 * @property snackBarMessage
 * @property showSortByDialog
 * @property showFilterDialog
 */
data class AlbumsViewState(
    val albums: List<UIAlbum> = emptyList(),
    val currentAlbum: Album? = null,
    val selectedPhotoIds: Set<Long> = emptySet(),
    val currentSort: Sort = Sort.NEWEST,
    val currentMediaType: FilterMediaType = FilterMediaType.ALL_MEDIA,
    val createAlbumPlaceholderTitle: String = "",
    val isInputNameValid: Boolean = true,
    val createDialogErrorMessage: Int? = null,
    val isAlbumCreatedSuccessfully: Boolean = false,
    val snackBarMessage: String = "",
    val showSortByDialog: Boolean = false,
    val showFilterDialog: Boolean = false,
)
package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo

/**
 * @property albums
 * @property currentAlbum
 * @property selectedPhotos
 * @property currentSort
 * @property currentMediaType
 * @property newAlbumTitleInput
 * @property createAlbumPlaceholderTitle
 * @property isInputNameValid
 * @property createDialogErrorMessage
 * @property isAlbumCreatedSuccessfully
 * @property snackBarMessage
 * @property showCreateAlbumDialog
 * @property showSortByDialog
 * @property showFilterDialog
 * @property showRemovePhotosDialog
 * @property deletedAlbumIds
 * @property albumDeletedMessage
 * @property showDeleteAlbumsConfirmation
 * @property selectedAlbumIds
 * @property showAlbums
 * @property showRenameDialog
 */
data class AlbumsViewState(
    val albums: List<UIAlbum> = emptyList(),
    val currentAlbum: Album? = null,
    val selectedPhotos: Set<Photo> = emptySet(),
    val currentSort: Sort = Sort.NEWEST,
    val currentMediaType: FilterMediaType = FilterMediaType.ALL_MEDIA,
    val newAlbumTitleInput: String = "",
    val createAlbumPlaceholderTitle: String = "",
    val isInputNameValid: Boolean = true,
    val createDialogErrorMessage: Int? = null,
    val isAlbumCreatedSuccessfully: Boolean = false,
    val snackBarMessage: String = "",
    val showCreateAlbumDialog: Boolean = false,
    val showSortByDialog: Boolean = false,
    val showFilterDialog: Boolean = false,
    val showRemovePhotosDialog: Boolean = false,
    val deletedAlbumIds: Set<AlbumId> = setOf(),
    val albumDeletedMessage: String = "",
    val showDeleteAlbumsConfirmation: Boolean = false,
    val selectedAlbumIds: Set<AlbumId> = setOf(),
    val showAlbums: Boolean = false,
    val showRenameDialog: Boolean = false,
) {
    val currentUIAlbum: UIAlbum?
        get() {
            val currentAlbum = currentAlbum ?: return null
            return if (currentAlbum !is Album.UserAlbum) {
                albums.find { uiAlbum -> uiAlbum.id == currentAlbum }
            } else {
                findUIAlbum(currentAlbum.id)
            }
        }

    val currentUserAlbum: Album.UserAlbum?
        get() = currentUIAlbum?.id as? Album.UserAlbum

    fun findUIAlbum(albumId: AlbumId): UIAlbum? {
        return albums.find { uiAlbum -> (uiAlbum.id as? Album.UserAlbum)?.id == albumId }
    }
}
package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId

/**
 * @property albums
 * @property currentAlbum
 * @property newAlbumTitleInput
 * @property createAlbumPlaceholderTitle
 * @property isInputNameValid
 * @property createDialogErrorMessage
 * @property isAlbumCreatedSuccessfully
 * @property showCreateAlbumDialog
 * @property deletedAlbumIds
 * @property albumDeletedMessage
 * @property showDeleteAlbumsConfirmation
 * @property showRemoveAlbumLinkDialog
 * @property removedLinksCount
 * @property selectedAlbumIds
 * @property showAlbums
 */
data class AlbumsViewState(
    val albums: List<UIAlbum> = emptyList(),
    val currentAlbum: Album? = null,
    val newAlbumTitleInput: String = "",
    val createAlbumPlaceholderTitle: String = "",
    val isInputNameValid: Boolean = true,
    val createDialogErrorMessage: Int? = null,
    val isAlbumCreatedSuccessfully: Boolean = false,
    val showCreateAlbumDialog: Boolean = false,
    val deletedAlbumIds: Set<AlbumId> = setOf(),
    val albumDeletedMessage: String = "",
    val showDeleteAlbumsConfirmation: Boolean = false,
    val showRemoveAlbumLinkDialog: Boolean = false,
    val removedLinksCount: Int = 0,
    val selectedAlbumIds: Set<AlbumId> = setOf(),
    val showAlbums: Boolean = false,
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

    fun findUIAlbum(albumId: AlbumId): UIAlbum? {
        return albums.find { uiAlbum -> (uiAlbum.id as? Album.UserAlbum)?.id == albumId }
    }
}
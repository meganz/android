package mega.privacy.android.app.presentation.photos.albums.importlink

import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo

internal data class AlbumImportState(
    val isInitialized: Boolean = false,
    val isLogin: Boolean = false,
    val showInputDecryptionKeyDialog: Boolean = false,
    val link: String? = null,
    val isLocalAlbumsLoaded: Boolean = false,
    val album: Album.UserAlbum? = null,
    val photos: List<Photo> = listOf(),
    val selectedPhotos: Set<Photo> = setOf(),
    val showErrorAccessDialog: Boolean = false,
)

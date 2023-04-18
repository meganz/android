package mega.privacy.android.app.presentation.photos.albums.getlink

import mega.privacy.android.domain.entity.photos.Album.UserAlbum

data class AlbumGetLinkState(
    val isInitialized: Boolean = false,
    val albumSummary: AlbumSummary? = null,
    val isSeparateKeyEnabled: Boolean = false,
    val link: String = "",
    val exitScreen: Boolean = false,
)

data class AlbumSummary(
    val album: UserAlbum,
    val numPhotos: Int,
)

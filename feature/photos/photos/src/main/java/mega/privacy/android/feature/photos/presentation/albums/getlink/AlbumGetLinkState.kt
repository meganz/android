package mega.privacy.android.feature.photos.presentation.albums.getlink

import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Album.UserAlbum

data class AlbumGetLinkState(
    val isInitialized: Boolean = false,
    val albumSummary: AlbumSummary? = null,
    val isSeparateKeyEnabled: Boolean = false,
    val link: String = "",
    val exitScreen: Boolean = false,
    val showCopyright: Boolean = false,
    val showSharingSensitiveWarning: Boolean = false,
    val copyrightAgreed: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
)

data class AlbumSummary(
    val album: UserAlbum,
    val numPhotos: Int,
)

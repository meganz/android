package mega.privacy.android.app.presentation.photos.search

import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.photos.Photo

internal data class PhotosSearchState(
    val isInitializing: Boolean = true,
    val query: String = "",
    val recentQueries: List<String> = listOf(),
    val selectedQuery: String? = null,
    val photos: List<Photo> = listOf(),
    val photosSource: List<Photo> = listOf(),
    val isSearchingPhotos: Boolean = false,
    val albums: List<UIAlbum> = listOf(),
    val albumsSource: List<UIAlbum> = listOf(),
    val isSearchingAlbums: Boolean = false,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
)

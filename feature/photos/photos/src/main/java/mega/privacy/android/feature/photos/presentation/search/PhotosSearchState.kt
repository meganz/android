package mega.privacy.android.feature.photos.presentation.search

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum

/**
 * Represents the content state of the media search screen.
 */
enum class MediaContentState {
    Loading,
    WelcomeEmpty,
    RecentQueries,
    NoResults,
    SearchResults,
}

data class PhotosSearchState(
    val isInitializing: Boolean = true,
    val contentState: MediaContentState = MediaContentState.Loading,
    val query: String = "",
    val recentQueries: List<String> = listOf(),
    val selectedQuery: String? = null,
    val photos: List<Photo> = listOf(),
    val photosSource: List<Photo> = listOf(),
    val isSearchingPhotos: Boolean = false,
    val legacyAlbums: List<UIAlbum> = listOf(),
    val legacyAlbumSource: List<UIAlbum> = listOf(),
    val albums: List<AlbumUiState> = listOf(),
    val albumSource: List<AlbumUiState> = listOf(),
    val isSearchingAlbums: Boolean = false,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val isSingleActivityEnabled: Boolean? = null,
)
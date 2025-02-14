package mega.privacy.android.app.presentation.photos.albums.add

import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.photos.Album.UserAlbum

internal data class AddToAlbumState(
    val viewType: Int = 0,
    val isLoadingAlbums: Boolean = true,
    val albums: List<Pair<UserAlbum, Int>> = listOf(),
    val selectedAlbum: UserAlbum? = null,
    val isCreatingAlbum: Boolean = false,
    val albumNameSuggestion: String = "",
    val albumNameErrorMessageRes: Int? = null,
    val isLoadingPlaylists: Boolean = true,
    val playlists: List<VideoPlaylistUIEntity> = listOf(),
    val selectedPlaylist: VideoPlaylistUIEntity? = null,
    val isCreatingPlaylist: Boolean = false,
    val playlistNameSuggestion: String = "",
    val playlistNameErrorMessageRes: Int? = null,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val mediaHolderName: String = "",
    val numAddedItems: Int = 0,
    val completionType: Int = -1,
    val additionType: Int = 0,
) {
    val existingAlbumNames: List<String>
        get() = albums.map { it.first.title }

    val existingPlaylistNames: List<String>
        get() = playlists.map { it.title }
}

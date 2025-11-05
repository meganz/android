package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import javax.inject.Inject

class AlbumUiStateMapper @Inject constructor(
    private val photoUiStateMapper: PhotoUiStateMapper,
) {

    /**
     * Maps a [MediaAlbum] domain entity to [AlbumUiState] for presentation layer.
     *
     * @param album The media album to map
     * @return The UI state representation of the album
     *
     * Note: For [MediaAlbum.System] albums, we use [String.hashCode] to generate a Long ID
     * from the album name because System albums are identified by their name (String) rather
     * than a numeric ID. This ensures System albums have a consistent numeric ID for UI
     * purposes, even though they don't have a native Long ID like User albums.
     */
    operator fun invoke(album: MediaAlbum): AlbumUiState {
        val (albumId, title) = when (album) {
            is MediaAlbum.System -> album.id.albumName.hashCode().toLong() to album.id.albumName
            is MediaAlbum.User -> album.id.id to album.title
        }

        return AlbumUiState(
            mediaAlbum = album,
            title = title,
            cover = album.cover?.let { photoUiStateMapper(it) }
        )
    }
}
package mega.privacy.android.app.presentation.photos.albums.model.mapper

import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo

/**
 * UIAlbumMapper
 */
interface LegacyUIAlbumMapper {
    operator fun invoke(photos: List<Photo>, album: Album, isLoadingDone: Boolean = false): UIAlbum
}


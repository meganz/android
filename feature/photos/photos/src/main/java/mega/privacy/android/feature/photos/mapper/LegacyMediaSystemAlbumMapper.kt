package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.presentation.albums.model.FavouriteSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.GifSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.RawSystemAlbum
import javax.inject.Inject

/**
 * Legacy mapper to map old [Album] into [MediaAlbum]
 *
 * This will be removed later
 */
@Deprecated("Will be removed in the next phase")
class LegacyMediaSystemAlbumMapper @Inject constructor(
    private val systemAlbums: Set<@JvmSuppressWildcards SystemAlbum>,
) {
    operator fun invoke(album: Album, cover: Photo?): MediaAlbum.System? {
        val systemAlbum = systemAlbums
            .find {
                when (album) {
                    is Album.GifAlbum -> it is GifSystemAlbum
                    is Album.FavouriteAlbum -> it is FavouriteSystemAlbum
                    is Album.RawAlbum -> it is RawSystemAlbum
                    else -> false
                }
            }
            ?: return null

        return MediaAlbum.System(
            id = systemAlbum,
            cover = cover
        )
    }
}
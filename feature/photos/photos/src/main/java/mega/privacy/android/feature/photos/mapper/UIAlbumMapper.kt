package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumTitle
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * UIAlbumMapper
 */
@Deprecated("Will be migrated to AlbumUiStateMapper")
class UIAlbumMapper @Inject constructor() {
    operator fun invoke(
        count: Int,
        imageCount: Int,
        videoCount: Int,
        cover: Photo?,
        defaultCover: Photo?,
        album: Album,
    ): UIAlbum {
        val title = when (album) {
            Album.FavouriteAlbum -> AlbumTitle.ResourceTitle(sharedResR.string.system_album_favourites_title)
            Album.GifAlbum -> AlbumTitle.ResourceTitle(sharedResR.string.system_album_gif_title)
            Album.RawAlbum -> AlbumTitle.ResourceTitle(sharedResR.string.system_album_raw_title)
            is Album.UserAlbum -> AlbumTitle.StringTitle(album.title)
        }

        return UIAlbum(
            title = title,
            count = count,
            imageCount = imageCount,
            videoCount = videoCount,
            coverPhoto = cover,
            defaultCover = defaultCover,
            id = album,
        )
    }
}
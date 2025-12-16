package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumTitle
import mega.privacy.android.feature.photos.presentation.albums.model.FavouriteSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.GifSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum
import mega.privacy.android.shared.resources.R as sharedResR
import javax.inject.Inject

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

    @Deprecated("Temporary to support PhotosSearch")
    operator fun invoke(album: MediaAlbum): UIAlbum {
        return when (album) {
            is MediaAlbum.System -> UIAlbum(
                title = AlbumTitle.StringTitle(album.id.albumName),
                count = 0,
                imageCount = 0,
                videoCount = 0,
                coverPhoto = album.cover,
                defaultCover = album.cover,
                id = when (album.id) {
                    is FavouriteSystemAlbum -> Album.FavouriteAlbum
                    is GifSystemAlbum -> Album.GifAlbum
                    else -> Album.RawAlbum
                },
            )

            is MediaAlbum.User -> UIAlbum(
                title = AlbumTitle.StringTitle(album.title),
                count = 0,
                imageCount = 0,
                videoCount = 0,
                coverPhoto = album.cover,
                defaultCover = album.cover,
                id = Album.UserAlbum(
                    id = album.id,
                    title = album.title,
                    cover = album.cover,
                    creationTime = album.creationTime,
                    modificationTime = album.modificationTime,
                    isExported = album.isExported
                ),
                isExported = album.isExported
            )
        }
    }
}
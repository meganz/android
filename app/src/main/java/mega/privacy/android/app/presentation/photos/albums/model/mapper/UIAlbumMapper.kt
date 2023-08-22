package mega.privacy.android.app.presentation.photos.albums.model.mapper

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.AlbumTitle
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import javax.inject.Inject

/**
 * UIAlbumMapper
 */
class UIAlbumMapper @Inject constructor() {
    operator fun invoke(
        count: Int,
        cover: Photo?,
        defaultCover: Photo?,
        album: Album,
    ): UIAlbum {
        val title = when (album) {
            Album.FavouriteAlbum -> AlbumTitle.ResourceTitle(R.string.title_favourites_album)
            Album.GifAlbum -> AlbumTitle.ResourceTitle(R.string.photos_album_title_gif)
            Album.RawAlbum -> AlbumTitle.ResourceTitle(R.string.photos_album_title_raw)
            is Album.UserAlbum -> AlbumTitle.StringTitle(album.title)
        }

        return UIAlbum(
            title = title,
            count = count,
            coverPhoto = cover,
            defaultCover = defaultCover,
            photos = listOf(),
            id = album,
            isLoadingDone = true,
        )
    }
}

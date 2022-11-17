package mega.privacy.android.app.presentation.photos.albums.model.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import javax.inject.Inject

/**
 * UIAlbumMapper
 */
interface UIAlbumMapper {
    operator fun invoke(photos: List<Photo>, album: Album): UIAlbum
}

/**
 * Mapper to convert list of Photos to UIAlbum
 */
class ToUIAlbum @Inject constructor(@ApplicationContext private val context: Context) :
    UIAlbumMapper {

    override fun invoke(photos: List<Photo>, album: Album): UIAlbum {
        val title = when (album) {
            Album.FavouriteAlbum -> context.getString(R.string.title_favourites_album)
            Album.GifAlbum -> context.getString(R.string.photos_album_title_gif)
            Album.RawAlbum -> context.getString(R.string.photos_album_title_raw)
            is Album.UserAlbum -> album.title
        }

        return UIAlbum(
            title = title,
            count = photos.size,
            coverPhoto = photos.maxByOrNull { it.modificationTime },
            photos = photos,
            id = album,
        )
    }
}
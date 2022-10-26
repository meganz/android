package mega.privacy.android.app.presentation.photos.albums.model.mapper

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo

/**
 * UIAlbumMapper
 */
typealias UIAlbumMapper = (@JvmSuppressWildcards List<@JvmSuppressWildcards Photo>, @JvmSuppressWildcards Album) -> @JvmSuppressWildcards UIAlbum

/**
 * Mapper to convert list of Photos to UIAlbum
 */
fun toUIAlbum(photos: List<Photo>, albumType: Album): UIAlbum {
    val title = when (albumType) {
        Album.FavouriteAlbum -> { context: Context -> context.getString(R.string.title_favourites_album) }
        Album.GifAlbum -> { context: Context -> context.getString(R.string.photos_album_title_gif) }
        Album.RawAlbum -> { context: Context -> context.getString(R.string.photos_album_title_raw) }
        is Album.UserAlbum -> { _ -> albumType.title }
    }

    return UIAlbum(
        title = title,
        count = photos.size,
        coverPhoto = photos.maxByOrNull { it.modificationTime },
        photos = photos,
        id = albumType,
    )
}

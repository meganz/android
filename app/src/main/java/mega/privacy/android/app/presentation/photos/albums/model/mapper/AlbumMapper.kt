package mega.privacy.android.app.presentation.photos.albums.model.mapper

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.domain.entity.photos.AlbumEntity
import mega.privacy.android.domain.entity.photos.Photo


typealias UIAlbumMapper = (@JvmSuppressWildcards List<@JvmSuppressWildcards Photo>, @JvmSuppressWildcards AlbumEntity) -> @JvmSuppressWildcards UIAlbum

/**
 * Mapper to convert list of Photos to UIAlbum
 */
fun toUIAlbum(photos: List<Photo>, albumType: AlbumEntity): UIAlbum {
    val title = when (albumType) {
        AlbumEntity.FavouriteAlbum -> { context: Context -> context.getString(R.string.title_favourites_album) }
        AlbumEntity.GifAlbum -> { context: Context -> context.getString(R.string.photos_album_title_gif) }
        AlbumEntity.RawAlbum -> { context: Context -> context.getString(R.string.photos_album_title_raw) }
        is AlbumEntity.UserAlbum -> { _ -> albumType.title }
    }

    return UIAlbum(
        title = title,
        count = photos.size,
        coverPhoto = photos.maxByOrNull { it.modificationTime },
        photos = photos,
        id = albumType,
    )
}

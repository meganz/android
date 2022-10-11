package mega.privacy.android.app.presentation.photos.albums.model.mapper

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.EntityAlbum
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.domain.entity.photos.Photo


typealias UIAlbumMapper = (@JvmSuppressWildcards List< @JvmSuppressWildcards Photo>, @JvmSuppressWildcards EntityAlbum) -> @JvmSuppressWildcards UIAlbum

fun toUIAlbum(photos: List<Photo>, albumType: EntityAlbum): UIAlbum {
    val title = when (albumType) {
        EntityAlbum.FavouriteAlbum -> { context: Context -> context.getString(R.string.title_favourites_album) }
        EntityAlbum.GifAlbum -> { context: Context -> context.getString(R.string.photos_album_title_gif) }
        EntityAlbum.RawAlbum -> { context: Context -> context.getString(R.string.photos_album_title_raw) }
        is EntityAlbum.UserAlbum -> { _ -> albumType.title }
    }

    return UIAlbum(
        title = title,
        count = photos.size,
        coverPhoto = photos.firstOrNull(),
        photos = photos,
        id = albumType,
    )
}

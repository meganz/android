package mega.privacy.android.app.presentation.photos.albums.model

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo

internal val Album.titleId: Int
    get() = when (this) {
        is Album.FavouriteAlbum -> R.string.title_favourites_album
    }

data class UIAlbum(
    val id: EntityAlbum,
    val title: (Context) -> String,
    val count: Int,
    val coverPhoto: Photo?,
    val photos: List<Photo>,
)

sealed interface EntityAlbum {
    object FavouriteAlbum : EntityAlbum
    object GifAlbum : EntityAlbum
    object RawAlbum : EntityAlbum
    data class UserAlbum(val id: Long, val title: String) : EntityAlbum
}
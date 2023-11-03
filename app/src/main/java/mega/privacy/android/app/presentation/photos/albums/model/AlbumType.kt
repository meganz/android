package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.domain.entity.photos.Album

enum class AlbumType {
    Favourite,
    Gif,
    Raw,
    Custom,
}

fun Album.getAlbumType() = when (this) {
    Album.FavouriteAlbum -> AlbumType.Favourite
    Album.GifAlbum -> AlbumType.Gif
    Album.RawAlbum -> AlbumType.Raw
    is Album.UserAlbum -> AlbumType.Custom
}
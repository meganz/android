package mega.privacy.android.feature.photos.presentation.albums.content

import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import mega.privacy.android.feature.photos.presentation.albums.model.FavouriteSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.GifSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.RawSystemAlbum

fun MediaAlbum.toAlbumContentNavKey(): AlbumContentNavKey {
    return AlbumContentNavKey(
        id = when (this) {
            is MediaAlbum.User -> this.id.id
            else -> null
        },
        type = when (this) {
            is MediaAlbum.System -> {
                when (this.id) {
                    is RawSystemAlbum -> "raw"
                    is GifSystemAlbum -> "gif"
                    is FavouriteSystemAlbum -> "favourite"
                    else -> null
                }
            }

            else -> "custom"
        }
    )
}
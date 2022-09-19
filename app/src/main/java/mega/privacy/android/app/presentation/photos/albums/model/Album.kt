package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.photos.Album

internal val Album.titleId: Int
    get() = when (this) {
        is Album.FavouriteAlbum -> R.string.title_favourites_album
    }

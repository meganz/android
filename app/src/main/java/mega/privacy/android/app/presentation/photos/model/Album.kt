package mega.privacy.android.app.presentation.photos.model

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.Album

internal val Album.titleId: Int
    get() = when (this) {
        is Album.FavouriteAlbum -> R.string.title_favourites_album
    }

package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.AlbumCoverItem

/**
 * Create an empty favorite album with it's unique id.
 */
fun createEmptyFavAlbum() =
    AlbumCoverItem(titleResId = R.string.title_favourites_album, coverThumbnail = null)





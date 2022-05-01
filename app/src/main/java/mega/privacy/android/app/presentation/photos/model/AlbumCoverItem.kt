package mega.privacy.android.app.presentation.photos.model

import nz.mega.sdk.MegaNode

const val ALBUM_ID_FAV = "favorite"

data class AlbumCoverItem(
    var albumId: String,
    var node: MegaNode? = null,
    var itemCount: Int = 0
)

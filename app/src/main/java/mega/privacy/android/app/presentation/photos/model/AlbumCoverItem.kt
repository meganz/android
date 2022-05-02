package mega.privacy.android.app.presentation.photos.model

const val ALBUM_ID_FAV = "favorite"

data class AlbumCoverItem(
    var albumId: String,
    var handle: Long? = null,
    var nodeBase64Handle: String? = "",
    var itemCount: Int = 0
)

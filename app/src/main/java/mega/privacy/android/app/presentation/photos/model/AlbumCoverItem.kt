package mega.privacy.android.app.presentation.photos.model

import java.io.File

data class AlbumCoverItem(
    val titleResId: Int,
    val coverThumbnail: File?,
    val itemCount: String = "0"
)

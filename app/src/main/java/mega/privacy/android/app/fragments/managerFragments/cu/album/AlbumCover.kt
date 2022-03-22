package mega.privacy.android.app.fragments.managerFragments.cu.album

import java.io.File

/**
 * AlbumCover is used for AlbumsFragment each cover.
 */
data class AlbumCover(
    var thumbnail: File? = null,
    var count: Int = 0,
    var title: String = "Album"
)

package mega.privacy.android.domain.entity.photos

import java.io.File

/**
 * A general Album interface include a cover thumbnail, album items count and data classes for specific Album categories
 */
sealed interface Album {
    /**
     * Cover image
     */
    val thumbnail: File?

    /**
     *  album items count
     */
    val itemCount: Int

    /**
     * Favourite Album
     */
    data class FavouriteAlbum(
        override val thumbnail: File?,
        override val itemCount: Int,
    ) : Album
}
package mega.privacy.android.domain.entity.media

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo

sealed interface MediaAlbum {
    val cover: Photo?

    /**
     * System album (Favourite, GIF, RAW, etc.)
     *
     * @property id The system album
     * @property cover Cover photo for the album
     */
    data class System(
        val id: SystemAlbum,
        override val cover: Photo?,
    ) : MediaAlbum

    /**
     * User album
     *
     * @property id
     * @property title
     * @property cover
     * @property creationTime
     * @property modificationTime
     * @property isExported
     */
    data class User(
        val id: AlbumId,
        val title: String,
        val creationTime: Long,
        val modificationTime: Long,
        val isExported: Boolean,
        override val cover: Photo?,
    ) : MediaAlbum
}
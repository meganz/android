package mega.privacy.android.domain.entity.media

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.photos.AlbumId

sealed interface MediaAlbum {
    val cover: TypedFileNode?

    /**
     * System album (Favourite, GIF, RAW, etc.)
     *
     * @property id The system album
     * @property cover Cover node for the album
     */
    data class System(
        val id: SystemAlbum,
        override val cover: TypedFileNode?,
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
        override val cover: TypedFileNode?,
    ) : MediaAlbum
}
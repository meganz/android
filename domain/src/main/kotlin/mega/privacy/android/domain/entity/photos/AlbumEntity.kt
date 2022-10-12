package mega.privacy.android.domain.entity.photos
/**
 * Entity album
 *
 * @constructor Create empty Entity album
 */
sealed interface AlbumEntity {
    /**
     * Favourite
     */
    object FavouriteAlbum : AlbumEntity

    /**
     * Gif
     */
    object GifAlbum : AlbumEntity

    /**
     * Raw
     */
    object RawAlbum : AlbumEntity

    /**
     * User album
     *
     * @property id
     * @property title
     */
    data class UserAlbum(val id: Long, val title: String) : AlbumEntity
}

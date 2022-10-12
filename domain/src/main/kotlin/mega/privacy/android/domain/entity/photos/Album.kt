package mega.privacy.android.domain.entity.photos
/**
 * Entity album
 *
 * @constructor Create empty Entity album
 */
sealed interface Album {
    /**
     * Favourite
     */
    object FavouriteAlbum : Album

    /**
     * Gif
     */
    object GifAlbum : Album

    /**
     * Raw
     */
    object RawAlbum : Album

    /**
     * User album
     *
     * @property id
     * @property title
     */
    data class UserAlbum(val id: Long, val title: String) : Album
}

package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.PhotoPredicate
import javax.inject.Inject

/**
 * Get default albums map
 */
class DefaultGetDefaultAlbumsMap @Inject constructor(
    private val filterFavourite: FilterFavourite,
    private val filterGIF: FilterGIF,
    private val filterRAW: FilterRAW,
) : GetDefaultAlbumsMap {
    override fun invoke(): Map<Album, PhotoPredicate> {
        return linkedMapOf(
            Album.FavouriteAlbum to filterFavourite(),
            Album.GifAlbum to filterGIF(),
            Album.RawAlbum to filterRAW(),
        )
    }
}
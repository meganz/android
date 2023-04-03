package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.PhotoPredicate
import javax.inject.Inject

/**
 * Get default albums map
 */
class GetDefaultAlbumsMapUseCase @Inject constructor(
    private val filterFavouriteUseCase: FilterFavouriteUseCase,
    private val filterGIFUseCase: FilterGIFUseCase,
    private val filterRAWUseCase: FilterRAWUseCase,
) {
    /**
     * Get default albums map
     */
    operator fun invoke(): Map<Album, PhotoPredicate> {
        return linkedMapOf(
            Album.FavouriteAlbum to filterFavouriteUseCase(),
            Album.GifAlbum to filterGIFUseCase(),
            Album.RawAlbum to filterRAWUseCase(),
        )
    }
}
package mega.privacy.android.domain.usecase

import AlbumEntity
import mega.privacy.android.domain.entity.photos.PhotoPredicate
import javax.inject.Inject

class DefaultGetDefaultAlbumsMap @Inject constructor(
    private val filterFavourite: FilterFavourite,
    private val filterGIF: FilterGIF,
    private val filterRAW: FilterRAW,
) : GetDefaultAlbumsMap {
    override fun invoke(): Map<AlbumEntity, PhotoPredicate> {
        return linkedMapOf(
            AlbumEntity.FavouriteAlbum to filterFavourite(),
            AlbumEntity.GifAlbum to filterGIF(),
            AlbumEntity.RawAlbum to filterRAW(),
        )
    }
}
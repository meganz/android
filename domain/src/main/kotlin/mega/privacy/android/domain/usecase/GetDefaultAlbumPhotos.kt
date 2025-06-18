package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.photos.Photo

/**
 * Get Default Album(Favourite GIF RAW) Photos
 */
interface GetDefaultAlbumPhotos {

    /**
     * Get Default Album(Favourite GIF RAW) Photos
     */
    operator fun invoke(
        isPaginationEnabled: Boolean,
        list: List<suspend (Photo) -> Boolean>
    ): Flow<List<Photo>>
}
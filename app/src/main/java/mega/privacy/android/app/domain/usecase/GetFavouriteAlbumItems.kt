package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.AlbumItemInfo

/**
 * The use case interface to get favourite album cover info
 */
interface GetFavouriteAlbumItems {
    /**
     * get favourites image and video under cu and mu
     * @return Flow<List<AlbumItemInfo>>
     */
    operator fun invoke(): Flow<List<AlbumItemInfo>>
}
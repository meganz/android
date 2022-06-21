package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FavouriteInfo

/**
 * The use case interface to get favourites
 */
fun interface GetAllFavorites {
    /**
     * get favourites
     * @return Flow<List<FavouriteInfo>>
     */
    operator fun invoke(): Flow<List<FavouriteInfo>>
}
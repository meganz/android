package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.Node

/**
 * The use case interface to get favourites
 */
fun interface GetAllFavorites {
    /**
     * get favourites
     * @return Flow<List<FavouriteInfo>>
     */
    operator fun invoke(): Flow<List<Node>>
}
package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.node.Node

/**
 * The repository interface regarding favourites
 */
interface FavouritesRepository {

    /**
     * Get favourites
     * @return List<FavouriteInfo>
     */
    suspend fun getAllFavorites(): List<Node>

    /**
     * Get children nodes by node
     * @param parentHandle the parent node handle
     * @return FavouriteFolderInfo
     */
    suspend fun getChildren(parentHandle: Long): FavouriteFolderInfo?

    /**
     * Monitor the node change
     * @return Flow<Boolean>
     */
    fun monitorNodeChange(): Flow<Boolean>

    /**
     * Removing favourites
     * @param handles the handle of items that are removed.
     */
    suspend fun removeFavourites(handles: List<Long>)
}
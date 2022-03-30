package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FavouriteFolderInfo
import mega.privacy.android.app.domain.entity.FavouriteInfo

/**
 * The repository interface regarding favourites
 */
interface FavouritesRepository {

    /**
     * Get favourites
     * @return List<FavouriteInfo>
     */
    suspend fun getAllFavorites(): List<FavouriteInfo>

    /**
     * Get children nodes by node
     * @param parentHandle the parent node handle
     * @return FavouriteFolderInfo
     */
    suspend fun getChildren(parentHandle: Long): FavouriteFolderInfo

    /**
     * Monitor the node change
     * @return Flow<Boolean>
     */
    fun monitorNodeChange(): Flow<Boolean>
}
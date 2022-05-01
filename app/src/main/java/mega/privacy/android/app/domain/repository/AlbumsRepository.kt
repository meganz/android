package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.AlbumItemInfo

/**
 * The repository interface regarding albums
 */
interface AlbumsRepository {

    /**
     * Get favourites album items
     * @return List<AlbumItemInfo>
     */
    suspend fun getFavouriteAlbumItems(): List<AlbumItemInfo>

    /**
     * Monitor the node change
     * @return Flow<Boolean>
     */
    fun monitorNodeChange(): Flow<Boolean>

}
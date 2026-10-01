package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * The repository interface regarding favourites
 */
interface FavouritesRepository {

    /**
     * Adding favourites
     * @param nodeIds the node id of items that are added.
     */
    suspend fun addFavourites(nodeIds: List<NodeId>)

    /**
     * Get favourites
     *
     * @param excludeSensitives When true, the SDK search filter excludes sensitive (hidden)
     *   favourites so the UI never sees them. Defaults to false.
     * @return List<FavouriteInfo>
     */
    suspend fun getAllFavorites(excludeSensitives: Boolean = false): List<UnTypedNode>

    /**
     * Removing favourites
     * @param nodeIds the nodeId of items that are removed.
     */
    suspend fun removeFavourites(nodeIds: List<NodeId>)
}
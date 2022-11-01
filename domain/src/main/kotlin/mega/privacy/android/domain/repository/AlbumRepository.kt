package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.set.UserSet

/**
 * Album repository
 */
interface AlbumRepository {
    /**
     * Get all user sets
     */
    suspend fun getAllUserSets(): List<UserSet>

    /**
     * Get album element ids
     */
    suspend fun getAlbumElementIDs(albumId: AlbumId): List<NodeId>
}

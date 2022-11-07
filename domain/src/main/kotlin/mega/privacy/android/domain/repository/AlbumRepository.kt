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
     *
     * @return a list of UserSet
     */
    suspend fun getAllUserSets(): List<UserSet>

    /**
     * Get album element ids
     *
     * @param albumId the id of the album which elements we want to get
     *
     * @return a list of node id's
     */
    suspend fun getAlbumElementIDs(albumId: AlbumId): List<NodeId>

    /**
     * Create an album
     *
     * @param name the name of the album
     */
    suspend fun createAlbum(name: String): UserSet

    /**
     * Add photos to an album
     *
     * @param albumID the id of the album which we want to put the photos in
     * @param photoIDs the photos' node handles
     */
    suspend fun addPhotosToAlbum(albumID: AlbumId, photoIDs: List<NodeId>)
}

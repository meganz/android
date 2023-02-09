package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.AlbumPhotosAddingProgress
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
     * Get a user set
     *
     * @param albumId is the album's id to get the user set
     * @return the user set if exist
     */
    suspend fun getUserSet(albumId: AlbumId): UserSet?

    /**
     * Get album element ids
     *
     * @param albumId the id of the album which elements we want to get
     *
     * @return a list of node id's
     */
    suspend fun getAlbumElementIDs(albumId: AlbumId): List<AlbumPhotoId>

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

    /**
     * Remove photos from an album
     *
     * @param albumID the id of the album which we want to remove the elements from
     * @param photoIDs the photos' set of IDs to be removed from the album
     */
    suspend fun removePhotosFromAlbum(albumID: AlbumId, photoIDs: List<AlbumPhotoId>)

    /**
     * Monitor user sets update
     *
     * @return a flow of all new user sets update
     */
    fun monitorUserSetsUpdate(): Flow<List<UserSet>>

    /**
     * Monitor user set's element ids update
     *
     * @param albumId the id of the album which we want to map the ids associated with
     * @return a flow of all new element ids update
     */
    fun monitorAlbumElementIds(albumId: AlbumId): Flow<List<AlbumPhotoId>>

    /**
     * Remove user albums
     * @param albumIds the album ids to be removed
     * @return throw exception if the operation fails
     */
    suspend fun removeAlbums(albumIds: List<AlbumId>)

    /**
     * Observe album photos adding progress
     *
     * @param albumId the album id to be observed its photos adding progress
     * @return a flow of progress
     */
    fun observeAlbumPhotosAddingProgress(albumId: AlbumId): Flow<AlbumPhotosAddingProgress?>

    /**
     * Update to acknowledge album photos adding progress is completed
     *
     * @param albumId the album id to be observed its photos adding progress
     */
    suspend fun updateAlbumPhotosAddingProgressCompleted(albumId: AlbumId)

    /**
     * Update album name
     *
     * @param albumId the album id
     * @param newName new album name
     */
    suspend fun updateAlbumName(
        albumId: AlbumId,
        newName: String,
    ): String

    /**
     * Get all the names that User Albums are not allowed to have
     */
    suspend fun getProscribedAlbumTitles(): List<String>
}

package mega.privacy.android.app.domain.repository

import java.io.File

/**
 * Albums repository
 */
interface AlbumsRepository {

    /**
     * Get Camera Upload Folder handle
     *
     * @return
     */
    suspend fun getCameraUploadFolderId(): Long?

    /**
     * Get Media Upload Folder handle
     *
     * @return
     */
    suspend fun getMediaUploadFolderId(): Long?


    /**
     * Check thumbnail from local
     *
     * @param nodeId
     * @return The thumbnail file associated with the node, null if doesn't exist in local
     */
    suspend fun getThumbnailFromLocal(nodeId: Long): File?

    /**
     * Check thumbnail from server
     *
     * @param nodeId
     * @return The thumbnail file associated with the node, null if can't be retrieved
     */
    suspend fun getThumbnailFromServer(nodeId: Long): File?
}
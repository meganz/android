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
     */
    suspend fun getThumbnailFromLocal(nodeId: Long): File?

    /**
     * Check thumbnail from server
     */
    suspend fun getThumbnailFromServer(nodeId: Long): File
}
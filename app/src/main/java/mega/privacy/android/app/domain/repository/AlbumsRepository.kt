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
    fun getCameraUploadFolderId(): Long?

    /**
     * Get Media Upload Folder handle
     *
     * @return
     */
    fun getMediaUploadFolderId(): Long?


    /**
     * Check thumbnail from local
     */
    fun getThumbnailFromLocal(nodeId: Long): File?

    /**
     * Check thumbnail from server
     */
    suspend fun getThumbnailFromServer(nodeId: Long): File
}
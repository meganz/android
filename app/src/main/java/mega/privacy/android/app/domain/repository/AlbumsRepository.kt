package mega.privacy.android.app.domain.repository

import java.io.File

interface AlbumsRepository {

    /**
     * Get Camera Upload Folder handle
     *
     * @return
     */
    fun getCameraUploadFolder(): String?

    /**
     * Get Media Upload Folder handle
     *
     * @return
     */
    fun getMediaUploadFolder(): String?


    /**
     * Check thumbnail from local
     */
    fun getThumbnailFromLocal(thumbnailName: String): File?

    /**
     * Check thumbnail from server
     */
    suspend fun getThumbnailFromServer(nodeId: Long,thumbnailName: String): File
}
package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
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
     * Check thumbnail
     */
    suspend fun getThumbnail(nodeId: Long, base64Handle: String): File
}
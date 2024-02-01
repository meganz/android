package mega.privacy.android.domain.repository.thumbnailpreview

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.exception.MegaException
import java.io.File

/**
 * Thumbnail preview repository.
 */
interface ThumbnailPreviewRepository {

    /**
     * Check thumbnail from local
     * @param handle node handle
     * @return thumbnail file
     */
    suspend fun getThumbnailFromLocal(handle: Long): File?

    /**
     * Check public node thumbnail from local
     * @param handle node handle
     * @return thumbnail file
     */
    suspend fun getPublicNodeThumbnailFromLocal(handle: Long): File?

    /**
     * Check thumbnail from server
     * @param handle node handle
     * @return thumbnail file
     */
    @Throws(MegaException::class)
    suspend fun getThumbnailFromServer(handle: Long): File?

    /**
     * Check public node thumbnail from server
     * @param handle node handle
     * @return thumbnail file
     */
    @Throws(MegaException::class)
    suspend fun getPublicNodeThumbnailFromServer(handle: Long): File?

    /**
     * Check preview from local
     * @param handle node handle
     * @return preview file
     */
    suspend fun getPreviewFromLocal(handle: Long): File?

    /**
     * Check preview from server
     * @param typedNode
     * @return preview file
     */
    suspend fun getPreviewFromServer(typedNode: TypedNode): File?

    /**
     * Download thumbnail
     *
     * @param handle
     * @param callback is download success
     */
    suspend fun downloadThumbnail(handle: Long, callback: (success: Boolean) -> Unit)

    /**
     * Download preview
     *
     * @param handle
     * @param callback is download success
     */
    suspend fun downloadPreview(handle: Long, callback: (success: Boolean) -> Unit)

    /**
     * Download public node thumbnail
     *
     * @param handle
     */
    suspend fun downloadPublicNodeThumbnail(handle: Long): Boolean

    /**
     * Download public node preview
     *
     * @param handle
     */
    suspend fun downloadPublicNodePreview(handle: Long): Boolean

    /**
     * Get Thumbnail Cache Folder Path
     */
    suspend fun getThumbnailCacheFolderPath(): String?

    /**
     * Get Preview Cache Folder Path
     */
    suspend fun getPreviewCacheFolderPath(): String?

    /**
     * Get Full Image Cache Folder Path
     */
    suspend fun getFullSizeCacheFolderPath(): String?


    /**
     * Create a thumbnail for an image
     *
     * @param handle
     * @param file
     * @return True if the thumbnail was successfully created, otherwise false.
     */

    suspend fun createThumbnail(handle: Long, file: File): Boolean

    /**
     * Create a preview for an image
     *
     * @param handle
     * @param file
     * @return True if the preview was successfully created, otherwise false.
     */

    suspend fun createPreview(handle: Long, file: File): Boolean

    /**
     * Create a preview for an image or video
     *
     * @param name
     * @param file
     * @return True if the preview was successfully created, otherwise false.
     */

    suspend fun createPreview(name: String, file: File): Boolean

    /**
     * Delete a thumbnail for node
     *
     * @param handle
     * @return True if the thumbnail was successfully created, otherwise false.
     */

    suspend fun deleteThumbnail(handle: Long): Boolean?

    /**
     * Create a preview for an image
     *
     * @param handle
     */

    suspend fun deletePreview(handle: Long): Boolean?

    /**
     * Get thumbnail or preview file name based on the node handle.
     *
     * @param nodeHandle
     * @return The name.
     */
    suspend fun getThumbnailOrPreviewFileName(nodeHandle: Long): String

    /**
     * Get thumbnail or preview file name based on the string.
     *
     * @param name
     * @return The name.
     */
    suspend fun getThumbnailOrPreviewFileName(name: String): String

    /**
     * Set the thumbnail of a MegaNode
     *
     * @param nodeHandle MegaNode handle to set the thumbnail
     * @param srcFilePath Source path of the file that will be set as thumbnail
     */
    suspend fun setThumbnail(nodeHandle: Long, srcFilePath: String)

    /**
     * Set the preview of a MegaNode
     *
     * @param nodeHandle MegaNode handle to set the preview
     * @param srcFilePath Source path of the file that will be set as preview
     */
    suspend fun setPreview(nodeHandle: Long, srcFilePath: String)
}
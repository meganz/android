package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.exception.MegaException
import java.io.File

/**
 * The repository interface regarding thumbnail/preview feature.
 */
interface ImageRepository {

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
     * @param handle node handle
     * @return preview file
     */
    suspend fun getPreviewFromServer(handle: Long): File?

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
     * @param callback is download success
     */
    suspend fun downloadPublicNodeThumbnail(handle: Long): Boolean

    /**
     * Download public node preview
     *
     * @param handle
     * @param callback is download success
     */
    suspend fun downloadPublicNodePreview(handle: Long): Boolean

    /**
     * Get Image Result given Offline File
     *
     * @param offlineNodeInformation    OfflineNodeInformation
     * @param file                      Image Offline File
     * @param highPriority              Flag to request image with high priority
     *
     * @return ImageResult
     */
    suspend fun getImageByOfflineFile(
        offlineNodeInformation: OfflineNodeInformation,
        file: File,
        highPriority: Boolean,
    ): ImageResult

    /**
     * Get Image Result given File
     *
     * @param file                      Image File
     * @param highPriority              Flag to request image with high priority
     *
     * @return ImageResult
     */
    suspend fun getImageFromFile(
        file: File,
        highPriority: Boolean,
    ): ImageResult

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
     * Get ImageNode given Node Handle
     * @param handle                Image Node handle to request
     * @return ImageNode            Image Node
     */
    suspend fun getImageNodeByHandle(handle: Long): ImageNode

    /**
     * Get ImageNode given Public Link
     * @param nodeFileLink          Public link to a file in MEGA
     * @return ImageNode            Image Node
     */
    suspend fun getImageNodeForPublicLink(nodeFileLink: String): ImageNode

    /**
     * Get ImageNode given Chat Room Id and Chat Message Id
     * @param chatRoomId            Chat Room Id
     * @param chatMessageId         Chat Message Id
     * @return ImageNode            Image Node
     */
    suspend fun getImageNodeForChatMessage(
        chatRoomId: Long,
        chatMessageId: Long,
    ): ImageNode

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
}

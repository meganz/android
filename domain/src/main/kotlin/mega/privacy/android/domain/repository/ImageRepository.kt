package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult
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
     * Check thumbnail from server
     * @param handle node handle
     * @return thumbnail file
     */
    @Throws(MegaException::class)
    suspend fun getThumbnailFromServer(handle: Long): File?

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
     * Get Image Result given Node Handle
     * @param nodeHandle    Image Node handle to request.
     * @param fullSize      Flag to request full size image despite data/size requirements.
     * @param highPriority  Flag to request image with high priority.
     * @param isMeteredConnection   Boolean indicating if network connection is metered
     * @return Flow<ImageResult>
     */
    suspend fun getImageByNodeHandle(
        nodeHandle: Long,
        fullSize: Boolean,
        highPriority: Boolean,
        isMeteredConnection: Boolean,
    ): Flow<ImageResult>
}
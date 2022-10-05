package mega.privacy.android.domain.repository

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
}
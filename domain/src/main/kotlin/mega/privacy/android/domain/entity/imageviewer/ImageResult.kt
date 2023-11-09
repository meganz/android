package mega.privacy.android.domain.entity.imageviewer

import java.io.File
import java.net.URI

/**
 * Domain entity that encapsulates an image result coming from GetImageUseCase.
 *
 * @property thumbnailUri       Image thumbnail Uri.
 * @property previewUri         Image preview Uri.
 * @property fullSizeUri        Image full size Uri.
 * @property transferTag        Full Image Mega request tag to cancel if it's not needed anymore.
 * @property isVideo            Flag to check if it's a video.
 * @property isFullyLoaded      Flag to check if the image has been fully loaded.
 * @property totalBytes         Total size of image in Bytes
 * @property transferredBytes   Transferred bytes
 */
data class ImageResult constructor(
    var thumbnailUri: String? = null,
    var previewUri: String? = null,
    var fullSizeUri: String? = null,
    var transferTag: Int? = null,
    var isVideo: Boolean = false,
    var isFullyLoaded: Boolean = false,
    var totalBytes: Long? = null,
    var transferredBytes: Long? = null,
) {

    /**
     * Get highest resolution image available.
     *
     * @return  Image Uri or null.
     */
    fun getHighestResolutionAvailableUri(): String? =
        checkUri(fullSizeUri) ?: checkUri(previewUri) ?: checkUri(thumbnailUri)

    /**
     * Get lowest resolution image available.
     *
     * @return  Image Uri or null.
     */
    fun getLowestResolutionAvailableUri(): String? =
        checkUri(thumbnailUri) ?: checkUri(previewUri) ?: checkUri(fullSizeUri)

    /**
     * Get Progress percentage while Image is getting downloaded
     *
     * @return Int or null.
     */
    fun getProgressPercentage(): Int? {
        transferredBytes?.let { transferred ->
            totalBytes?.let { total ->
                if (total != 0L && transferred != 0L) {
                    return ((transferred * 1.0 / total) * 100).toInt()
                }
            }
        }
        return null
    }

    private fun checkUri(uriPath: String?): String? {
        return try {
            if (File(URI.create(uriPath).path).exists()) {
                uriPath
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

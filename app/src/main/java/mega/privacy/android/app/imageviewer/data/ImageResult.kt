package mega.privacy.android.app.imageviewer.data

import android.net.Uri

/**
 * Data object that encapsulates an image result coming from GetImageUseCase.
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
    var thumbnailUri: Uri? = null,
    var previewUri: Uri? = null,
    var fullSizeUri: Uri? = null,
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
    fun getHighestResolutionAvailableUri(): Uri? =
        fullSizeUri ?: previewUri ?: thumbnailUri

    /**
     * Get lowest resolution image available.
     *
     * @return  Image Uri or null.
     */
    fun getLowestResolutionAvailableUri(): Uri? =
        thumbnailUri ?: previewUri ?: fullSizeUri

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
}

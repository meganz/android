package mega.privacy.android.app.imageviewer.data

import android.net.Uri

/**
 * Data object that encapsulates an image result coming from GetImageUseCase.
 *
 * @property thumbnailUri   Image thumbnail Uri.
 * @property previewUri     Image preview Uri.
 * @property fullSizeUri    Image full size Uri.
 * @property transferTag    Full Image Mega request tag to cancel if it's not needed anymore.
 * @property isVideo        Flag to check if it's a video.
 * @property fullyLoaded    Flag to check if the image has been fully loaded.
 */
data class ImageResult(
    var thumbnailUri: Uri? = null,
    var previewUri: Uri? = null,
    var fullSizeUri: Uri? = null,
    var transferTag: Int? = null,
    var isVideo: Boolean = false,
    var fullyLoaded: Boolean = false
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
}

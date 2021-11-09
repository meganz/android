package mega.privacy.android.app.imageviewer.data

import android.net.Uri

data class ImageResult(
    var thumbnailUri: Uri? = null,
    var previewUri: Uri? = null,
    var fullSizeUri: Uri? = null,
    var transferTag: Int? = null,
    var isVideo: Boolean = false,
    var fullyLoaded: Boolean = false
) {
    fun getHighestResolutionAvailableUri(): Uri? =
        fullSizeUri ?: previewUri ?: thumbnailUri

    fun getLowestResolutionAvailableUri(): Uri? =
        thumbnailUri ?: previewUri ?: fullSizeUri
}

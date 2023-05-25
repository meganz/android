package mega.privacy.android.domain.entity.node

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageProgress

/**
 * Image node
 */
interface ImageNode : FileNode {
    /**
     * Download Thumbnail
     */
    val downloadThumbnail: suspend (String) -> String

    /**
     * Download Preview
     */
    val downloadPreview: suspend (String) -> String

    /**
     * Download Full Image
     */
    val downloadFullImage: (String, Boolean, () -> Unit) -> Flow<ImageProgress>

    /**
     * Latitude
     */
    val latitude: Double

    /**
     * Longitude
     */
    val longitude: Double
}
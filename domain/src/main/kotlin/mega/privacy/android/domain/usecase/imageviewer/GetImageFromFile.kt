package mega.privacy.android.domain.usecase.imageviewer

import mega.privacy.android.domain.entity.imageviewer.ImageResult
import java.io.File

/**
 * The use case interface to get Image Result given an Image file
 */
fun interface GetImageFromFile {

    /**
     * Get Image Result given an Image file
     * @param file              Image file
     * @param highPriority      Flag to request image with high priority
     *
     * @return ImageResult
     */
    suspend operator fun invoke(
        file: File,
        highPriority: Boolean,
    ): ImageResult
}
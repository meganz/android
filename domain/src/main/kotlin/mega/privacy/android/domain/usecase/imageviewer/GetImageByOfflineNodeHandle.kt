package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult

/**
 * The use case interface to get Image Result given Offline Node Handle
 */
fun interface GetImageByOfflineNodeHandle {

    /**
     * Get Image Result given Offline Node Handle
     * @param nodeHandle        Image Node handle to request
     * @param highPriority      Flag to request image with high priority
     *
     * @return ImageResult
     */
    suspend operator fun invoke(
        nodeHandle: Long,
        highPriority: Boolean,
    ): ImageResult
}
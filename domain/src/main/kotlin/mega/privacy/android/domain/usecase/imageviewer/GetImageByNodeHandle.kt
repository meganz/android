package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult

/**
 * The use case interface to get Image Result given Node Handle
 */
fun interface GetImageByNodeHandle {

    /**
     * Get Image Result given Node Handle
     * @param nodeHandle    Image Node handle to request.
     * @param fullSize      Flag to request full size image despite data/size requirements.
     * @param highPriority  Flag to request image with high priority.
     *
     * @return Flow<ImageResult>
     */
    suspend operator fun invoke(
        nodeHandle: Long,
        fullSize: Boolean,
        highPriority: Boolean
    ): Flow<ImageResult>?
}
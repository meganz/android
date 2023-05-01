package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * The use case to get Image Result given Node Handle
 */
class GetImageByNodeHandleUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val imageRepository: ImageRepository,
) {
    /**
     * Get Image Result given Node Handle
     *
     * @param nodeHandle        Image Node handle to request
     * @param fullSize          Flag to request full size image despite data/size requirements
     * @param highPriority      Flag to request image with high priority
     * @param resetDownloads    Callback to reset downloads
     *
     * @return Flow<ImageResult>
     */
    suspend operator fun invoke(
        nodeHandle: Long,
        fullSize: Boolean,
        highPriority: Boolean,
        resetDownloads: () -> Unit,
    ): Flow<ImageResult> {
        return imageRepository.getImageByNodeHandle(
            nodeHandle,
            fullSize,
            highPriority,
            networkRepository.isMeteredConnection() ?: false,
            resetDownloads
        )
    }
}
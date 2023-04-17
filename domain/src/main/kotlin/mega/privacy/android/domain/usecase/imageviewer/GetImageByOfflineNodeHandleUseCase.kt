package mega.privacy.android.domain.usecase.imageviewer

import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import javax.inject.Inject

/**
 * The use case to get Image Result given Offline Node Handle
 */
class GetImageByOfflineNodeHandleUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val imageRepository: ImageRepository,
) {
    /**
     * Invoke
     *
     * @param nodeHandle                Image Offline File node handle
     * @param highPriority              Flag to request image with high priority
     *
     * @return ImageResult
     */
    suspend operator fun invoke(
        nodeHandle: Long,
        highPriority: Boolean,
    ): ImageResult {
        nodeRepository.getOfflineNodeInformation(nodeHandle)?.let { nodeInformation ->
            getOfflineFileUseCase(nodeInformation).let { file ->
                if (!file.exists()) throw IllegalArgumentException("Offline file doesn't exist")
                return imageRepository.getImageByOfflineFile(
                    offlineNodeInformation = nodeInformation,
                    file = file,
                    highPriority = highPriority,
                )
            }
        } ?: throw IllegalArgumentException("Offline node was not found")
    }
}
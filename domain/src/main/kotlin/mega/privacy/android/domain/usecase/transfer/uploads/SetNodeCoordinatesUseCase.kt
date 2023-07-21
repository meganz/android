package mega.privacy.android.domain.usecase.transfer.uploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.file.GetGPSCoordinatesUseCase
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import javax.inject.Inject

/**
 * Set node coordinates use case
 *
 * @property nodeRepository
 * @property isVideoFileUseCase
 * @property isImageFileUseCase
 * @property getGPSCoordinatesUseCase
 * @constructor Create empty Set node coordinates use case
 */
class SetNodeCoordinatesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val isVideoFileUseCase: IsVideoFileUseCase,
    private val isImageFileUseCase: IsImageFileUseCase,
    private val getGPSCoordinatesUseCase: GetGPSCoordinatesUseCase,
) {

    /**
     * Invoke
     *
     * @param localPath File local path.
     * @param nodeHandle Node identifier of the file in the cloud.
     */
    suspend operator fun invoke(localPath: String, nodeHandle: Long) {
        val isVideo = isVideoFileUseCase(localPath)
        if (isVideo || isImageFileUseCase(localPath)) {
            val coordinates = getGPSCoordinatesUseCase(localPath, isVideo)
            nodeRepository.setNodeCoordinates(
                NodeId(nodeHandle),
                coordinates.first.toDouble(),
                coordinates.second.toDouble()
            )
        }
    }
}
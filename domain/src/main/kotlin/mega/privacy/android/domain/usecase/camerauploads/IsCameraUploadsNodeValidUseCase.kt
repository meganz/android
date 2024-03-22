package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import javax.inject.Inject

/**
 * Check if the Camera Uploads Node (Primary or secondary) is valid or node
 */
class IsCameraUploadsNodeValidUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
) {

    /**
     * Invocation function
     *
     * @param nodeId Check the validity of the Node with this id
     */
    suspend operator fun invoke(nodeId: NodeId) =
        nodeId.longValue != cameraUploadRepository.getInvalidHandle()
                && !isNodeInRubbishOrDeletedUseCase(nodeId.longValue)
}
